/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.investmentallocation.solver.move.factory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;

import org.optaplanner.core.impl.heuristic.move.CompositeMove;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.solver.random.RandomUtils;
import org.optaplanner.examples.investmentallocation.domain.AssetClassAllocation;
import org.optaplanner.examples.investmentallocation.domain.InvestmentAllocationSolution;
import org.optaplanner.examples.investmentallocation.domain.util.InvestmentAllocationNumericUtil;
import org.optaplanner.examples.investmentallocation.solver.move.InvestmentQuantityTransferMove;

public class InvestmentBiQuantityTransferMoveIteratorFactory implements MoveIteratorFactory {

    @Override
    public long getSize(ScoreDirector scoreDirector) {
        InvestmentAllocationSolution solution = (InvestmentAllocationSolution) scoreDirector.getWorkingSolution();
        int size = solution.getAssetClassAllocationList().size();
        // The MAXIMUM_QUANTITY_MILLIS accounts for all fromAllocations too
        return InvestmentAllocationNumericUtil.MAXIMUM_QUANTITY_MILLIS
                * InvestmentAllocationNumericUtil.MAXIMUM_QUANTITY_MILLIS
                * (size - 1) * (size - 1);
    }

    @Override
    public Iterator<Move> createOriginalMoveIterator(ScoreDirector scoreDirector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Move> createRandomMoveIterator(ScoreDirector scoreDirector, Random workingRandom) {
        InvestmentAllocationSolution solution = (InvestmentAllocationSolution) scoreDirector.getWorkingSolution();
        List<AssetClassAllocation> allocationList = solution.getAssetClassAllocationList();
        List<AssetClassAllocation> nonEmptyAllocationList = new ArrayList<AssetClassAllocation>(allocationList);
        for (Iterator<AssetClassAllocation> it = nonEmptyAllocationList.iterator(); it.hasNext(); ) {
            AssetClassAllocation allocation = it.next();
            if (allocation.getQuantityMillis() == 0L) {
                it.remove();
            }
        }
        return new RandomInvestmentBiQuantityTransferMoveIterator(allocationList,
                nonEmptyAllocationList, workingRandom);
    }

    private class RandomInvestmentBiQuantityTransferMoveIterator implements Iterator<Move> {

        private final List<AssetClassAllocation> allocationList;
        private final List<AssetClassAllocation> nonEmptyAllocationList;
        private final Random workingRandom;

        public RandomInvestmentBiQuantityTransferMoveIterator(List<AssetClassAllocation> allocationList,
                List<AssetClassAllocation> nonEmptyAllocationList, Random workingRandom) {
            this.allocationList = allocationList;
            this.nonEmptyAllocationList = nonEmptyAllocationList;
            this.workingRandom = workingRandom;
        }

        public boolean hasNext() {
            return allocationList.size() >= 3 && nonEmptyAllocationList.size() >= 1;
        }

        public Move next() {
            AssetClassAllocation firstFrom;
            AssetClassAllocation secondFrom;
            int nonEmptyAllocationListSize = nonEmptyAllocationList.size();
            if (nonEmptyAllocationListSize == 1) {
                firstFrom = nonEmptyAllocationList.get(0);
                secondFrom = firstFrom;
            } else {
                firstFrom = nonEmptyAllocationList.get(workingRandom.nextInt(nonEmptyAllocationListSize));
                // secondFrom can be the same as firstFrom, for example in a split from 1 into 2 others
                secondFrom = nonEmptyAllocationList.get(workingRandom.nextInt(nonEmptyAllocationListSize));
            }
            int allocationListSize = allocationList.size();
            int toCandidateSize = allocationListSize - (firstFrom == secondFrom ? 1 : 2);
            AssetClassAllocation firstTo = allocationList.get(workingRandom.nextInt(toCandidateSize));
            if (firstTo == firstFrom) {
                firstTo = allocationList.get(allocationListSize - 1);
            } else if (firstTo == secondFrom) {
                firstTo = allocationList.get(allocationListSize - 2);
            }
            // secondTo can be the same as firstTo, for example in a merge from 2 others into 1
            AssetClassAllocation secondTo = allocationList.get(workingRandom.nextInt(toCandidateSize));
            if (secondTo == firstFrom) {
                secondTo = allocationList.get(allocationListSize - 1);
            } else if (secondTo == secondFrom) {
                secondTo = allocationList.get(allocationListSize - 2);
            }
            long firstTransferMillis = RandomUtils.nextLong(workingRandom, firstFrom.getQuantityMillis()) + 1L;
            if (firstFrom == secondFrom && firstFrom.getQuantityMillis() == firstTransferMillis) {
                // secondTransferMillis must never do a nextLong(0L) which would throw an IllegalArgumentException
                firstTransferMillis--;
            }
            long secondTransferMillis = RandomUtils.nextLong(workingRandom, secondFrom.getQuantityMillis()
                    - (firstFrom == secondFrom ? firstTransferMillis : 0L)) + 1L;
            return CompositeMove.buildMove(new InvestmentQuantityTransferMove(firstFrom, firstTo, firstTransferMillis),
                    new InvestmentQuantityTransferMove(secondFrom, secondTo, secondTransferMillis));
        }

        public void remove() {
            throw new UnsupportedOperationException("The optional operation remove() is not supported.");
        }

    }

}
