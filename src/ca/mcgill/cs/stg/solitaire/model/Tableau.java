/*******************************************************************************
 * Solitaire
 *
 * Copyright (C) 2016 by Martin P. Robillard
 *
 * See: https://github.com/prmr/Solitaire
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package ca.mcgill.cs.stg.solitaire.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import ca.mcgill.cs.stg.solitaire.cards.Card;
import ca.mcgill.cs.stg.solitaire.cards.CardStack;
import ca.mcgill.cs.stg.solitaire.cards.Deck;
import ca.mcgill.cs.stg.solitaire.cards.Rank;

/**
 * Represents seven piles of cards that fan downwards, where cards
 * must be stacked in alternating suit colors, and where cards can 
 * be moved from pile to pile.
 */
class Tableau
{
	private Map<TableauPile, CardStack> aPiles = new HashMap<>();
	private Set<Card> aVisible = new HashSet<>();
	
	/**
	 * Creates an empty tableau.
	 */
	Tableau()
	{
		for( TableauPile index : TableauPile.values() )
		{
			aPiles.put(index, new CardStack());
		}
	}
	
	/**
	 * Fills the tableau by drawing cards from the deck.
	 * @param pDeck a deck of card to use to fill the piles initially.
	 * @pre pDeck != null
	 * 
	 */
	void initialize(Deck pDeck)
	{   
		assert pDeck != null; 
		aVisible.clear();
		for( int i = 0; i < TableauPile.values().length; i++ )
		{
			aPiles.get(TableauPile.values()[i]).clear();
			for( int j = 0; j < i+1; j++ )
			{
				Card card = pDeck.draw();
				aPiles.get(TableauPile.values()[i]).push(card);
				if( j == i )
				{
					aVisible.add(card);
				}
			}
		}
	}
	
	
	/**
	 * Determines if it is legal to move pCard on top of
	 * pile pIndex, i.e. if a king is moved to an empty
	 * pile or any other rank on a card of immediately greater
	 * rank but of a different color.
	 * @param pCard The card to move
	 * @param pIndex The destination pile.
	 * @return True if the move is legal.
	 * @pre pCard != null, pIndex != null
	 */
	boolean canMoveTo(Card pCard, TableauPile pIndex )
	{
		assert pCard != null && pIndex != null;
		CardStack stack = aPiles.get(pIndex);
		if( stack.isEmpty() )
		{
			return pCard.getRank() == Rank.KING;
		}
		else
		{ 
			return pCard.getRank().ordinal() == stack.peek().getRank().ordinal()-1 && 
					!pCard.getSuit().sameColorAs(stack.peek().getSuit());
		}
	}
	
	/**
	 * @param pIndex The index of the pile to obtain.
	 * @return An array of cards in the pile at pIndex, where element [0] is the 
	 * bottom of the stack. Modifying the array has no impact on the state of the 
	 * object.
	 * @pre pIndex != null
	 */
	Card[] getStack(TableauPile pIndex)
	{
		assert pIndex != null;
		return toArray(aPiles.get(pIndex));
	}
	
	private static Card[] toArray(CardStack pStack)
	{
		List<Card> result = new ArrayList<>();
		for( Card card : pStack)
		{
			result.add(card);
		}
		return result.toArray(new Card[result.size()]);
	}
	
	/**
	 * Returns true if moving pCard away reveals the top of the card.
	 * @param pCard The card to test
	 * @param pIndex The index of the pile.
	 * @return true if the card above pCard is not visible and pCard
	 * is visible.
	 * @pre pCard != null && pIndex != null
	 */
	boolean revealsTop(Card pCard, TableauPile pIndex)
	{
		assert pCard != null && pIndex != null;
		Optional<Card> previous = getPreviousCard(pCard, pIndex);
		if( !previous.isPresent() )
		{
			return false;
		}
		return aVisible.contains(pCard) && !aVisible.contains(previous.get());
	}
	
	private Optional<Card> getPreviousCard(Card pCard, TableauPile pIndex)
	{
		Optional<Card> previous = Optional.empty();
		for( Card card : aPiles.get(pIndex))
		{
			if( card == pCard )
			{
				return previous;
			}
			previous = Optional.of(card);
		}
		return Optional.empty();
	}
	
	
 	/**
	 * Move pCard and all the cards below to pDestination.
	 * @param pCard The card to move, possibly including all the cards on top of it.
	 * @param pOrigin The location of the card before the move.
	 * @param pDestination The intended destination of the card.
     * @pre this is a legal move
	 */
	void moveWithin(Card pCard, TableauPile pOrigin, TableauPile pDestination )
	{
		assert pCard != null && pOrigin != null && pDestination != null;
		assert contains(pCard, pOrigin);
		assert isVisible(pCard);
		Stack<Card> temp = new Stack<>();
		Card card = aPiles.get(pOrigin).pop();
		temp.push(card);
		while( card != pCard )
		{
			card = aPiles.get(pOrigin).pop();
			temp.push(card);
		}
		while( !temp.isEmpty() )
		{
			aPiles.get(pDestination).push(temp.pop());
		}
	}
	
	/**
	 * Returns a sequence of cards starting at pCard and including
	 * all cards on top of it.
	 * @param pCard The bottom card in the pile
	 * @param pIndex The index of the pile.
	 * @return An array of cards in the pile. Element 0 is the bottom.
	 * @pre pCard != null && pIndex != null
	 */
	public Card[] getSequence(Card pCard, TableauPile pIndex)
	{
		CardStack stack = aPiles.get(pIndex);
		List<Card> lReturn = new ArrayList<>();
		boolean aSeen = false;
		for( Card card : stack )
		{
			if( card == pCard )
			{
				aSeen = true;
			}
			if( aSeen )
			{
				lReturn.add(card);
			}
		}
		return lReturn.toArray(new Card[lReturn.size()]);
	}
	
	/**
	 * Make the top card of a pile visible.
	 * @param pIndex The index of the requested pile.
	 * @pre pIndex != null && !isEmpty(pIndex)
	 */
	void showTop(TableauPile pIndex)
	{
		assert !aPiles.get(pIndex).isEmpty();
		aVisible.add(aPiles.get(pIndex).peek());
	}
	
	/**
	 * Make the top card of a pile not visible.
	 * @param pIndex The index of the requested stack.
	 * @pre pIndex != null && !isEmpty(pIndex)
	 */
	void hideTop(TableauPile pIndex)
	{
		assert !aPiles.get(pIndex).isEmpty();
		aVisible.remove(aPiles.get(pIndex).peek());
	}
	
	/**
	 * @param pCard The card to check
	 * @param pIndex The index of the pile to check
	 * @return True if pIndex contains pCard
	 * @pre pCard != null && pIndex != null
	 */
	boolean contains(Card pCard, TableauPile pIndex)
	{
		assert pCard != null && pIndex != null;
		for( Card card : aPiles.get(pIndex))
		{
			if( card == pCard )
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param pCard The card to check.
	 * @return Whether pCard is contains in any stack.
	 * @pre pCard != null;
	 */
	boolean contains(Card pCard)
	{
		assert pCard != null;
		for( TableauPile index : TableauPile.values())
		{
			if( contains(pCard, index))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @param pCard The card to check.
	 * @return true if pCard is visible in the piles.
	 * @pre contains(pCard)
	 */
	boolean isVisible(Card pCard)
	{
		assert contains(pCard);
		return aVisible.contains(pCard);
	}
	
	/**
	 * Removes the top card from the pile at pIndex.
	 * @param pIndex The index of the pile to pop.
	 * @pre !isEmpty(pIndex)
	 */
	void pop(TableauPile pIndex)
	{
		assert !aPiles.get(pIndex).isEmpty();
		aVisible.remove(aPiles.get(pIndex).pop());
	}
	
	/**
	 * Places a card on top of the pile at pIndex. The
	 * card will be visible by default.
	 * @param pCard The card to push.
	 * @param pIndex The index of the destination stack.
	 * @pre pCard != null && pIndex != null;
	 */
	void push(Card pCard, TableauPile pIndex)
	{
		assert pCard != null && pIndex != null;
		aPiles.get(pIndex).push(pCard);
		aVisible.add(pCard);
	}
}