package org.geogebra.common.kernel.stepbystep.steps;

import static org.geogebra.common.kernel.stepbystep.steptree.StepExpression.closeToAnInteger;
import static org.geogebra.common.kernel.stepbystep.steptree.StepExpression.nonTrivialPower;
import static org.geogebra.common.kernel.stepbystep.steptree.StepExpression.nonTrivialProduct;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.abs;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.add;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.closeToAnInteger;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.divide;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.gcd;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.isEqual;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.isEven;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.isOdd;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.lcm;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.minus;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.multiply;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.power;
import static org.geogebra.common.kernel.stepbystep.steptree.StepNode.root;

import java.util.ArrayList;
import java.util.List;

import org.geogebra.common.kernel.stepbystep.solution.SolutionBuilder;
import org.geogebra.common.kernel.stepbystep.solution.SolutionStepType;
import org.geogebra.common.kernel.stepbystep.steptree.StepConstant;
import org.geogebra.common.kernel.stepbystep.steptree.StepExpression;
import org.geogebra.common.kernel.stepbystep.steptree.StepNode;
import org.geogebra.common.kernel.stepbystep.steptree.StepOperation;
import org.geogebra.common.plugin.Operation;

public enum RegroupSteps implements SimplificationStepGenerator {

	ELIMINATE_OPPOSITES {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.PLUS)) {
				StepOperation so = (StepOperation) sn;

				StepOperation newSum = new StepOperation(Operation.PLUS);

				boolean[] found = new boolean[so.noOfOperands()];
				for (int i = 0; i < so.noOfOperands(); i++) {
					if (so.getSubTree(i).isOperation(Operation.MINUS)
							&& ((StepOperation) so.getSubTree(i)).getSubTree(0).isOperation(Operation.PLUS)) {
						StepOperation innerSum = (StepOperation) ((StepOperation) so.getSubTree(i)).getSubTree(0);

						for (int j = 0; j < so.noOfOperands() - innerSum.noOfOperands(); j++) {
							boolean foundSum = true;
							for (int k = 0; foundSum && k < innerSum.noOfOperands(); k++) {
								if (!so.getSubTree(j + k).equals(innerSum.getSubTree(k))) {
									foundSum = false;
								}
							}

							if (foundSum) {
								found[i] = true;
								so.getSubTree(i).setColor(tracker.getColorTracker());

								for (int k = 0; k < innerSum.noOfOperands(); k++) {
									found[j + k] = true;
									so.getSubTree(j + k).setColor(tracker.getColorTracker());
								}
								sb.add(SolutionStepType.ELIMINATE_OPPOSITES, tracker.incColorTracker());
								break;
							}
						}
					}

					for (int j = i + 1; !found[i] && j < so.noOfOperands(); j++) {
						if (so.getSubTree(i).equals(so.getSubTree(j).negate())
								|| so.getSubTree(j).equals(so.getSubTree(i).negate())) {
							so.getSubTree(i).setColor(tracker.getColorTracker());
							so.getSubTree(j).setColor(tracker.getColorTracker());
							sb.add(SolutionStepType.ELIMINATE_OPPOSITES, tracker.incColorTracker());
							found[i] = true;
							found[j] = true;
						}
					}
				}

				for (int i = 0; i < so.noOfOperands(); i++) {
					if (!found[i]) {
						newSum.addSubTree(so.getSubTree(i));
					}
				}

				if (newSum.noOfOperands() == 0) {
					return new StepConstant(0);
				}

				if (newSum.noOfOperands() == 1) {
					return newSum.getSubTree(0);
				}

				return newSum;
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	EXPAND_ROOT {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				int rootCount = 0;
				long commonRoot = 1;
				for (int i = 0; i < so.noOfOperands(); i++) {
					if (so.getSubTree(i).isOperation(Operation.NROOT)) {
						double currentRoot = ((StepOperation) so.getSubTree(i)).getSubTree(1).getValue();
						if (closeToAnInteger(currentRoot)) {
							rootCount++;
							commonRoot = lcm(commonRoot, Math.round(currentRoot));
						}
					}
				}

				if (rootCount > 1) {
					StepOperation newProduct = new StepOperation(Operation.MULTIPLY);
					for (int i = 0; i < so.noOfOperands(); i++) {
						if (so.getSubTree(i).isOperation(Operation.NROOT)) {
							double currentRoot = ((StepOperation) so.getSubTree(i)).getSubTree(1).getValue();
							if (closeToAnInteger(currentRoot) && !isEqual(commonRoot, currentRoot)) {
								StepExpression argument = ((StepOperation) so.getSubTree(i)).getSubTree(0);

								StepExpression result = root(power(argument, commonRoot / currentRoot), commonRoot);

								so.getSubTree(i).setColor(tracker.getColorTracker());
								result.setColor(tracker.getColorTracker());

								sb.add(SolutionStepType.EXPAND_ROOT, tracker.incColorTracker());

								newProduct.addSubTree(result);
							} else {
								newProduct.addSubTree(so.getSubTree(i));
							}
						} else {
							newProduct.addSubTree(so.getSubTree(i));
						}
					}

					return newProduct;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	COMMON_ROOT {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				StepExpression newProduct = null;
				StepExpression underRoot = null;

				int rootCount = 0;
				double commonRoot = 0;
				for (int i = 0; i < so.noOfOperands(); i++) {
					if (so.getSubTree(i).isOperation(Operation.NROOT)) {
						double currentRoot = ((StepOperation) so.getSubTree(i)).getSubTree(1).getValue();
						if (isEqual(commonRoot, 0) || isEqual(commonRoot, currentRoot)) {
							commonRoot = currentRoot;
							underRoot = multiply(underRoot, (((StepOperation) so.getSubTree(i)).getSubTree(0)));
							rootCount++;
						} else {
							newProduct = multiply(newProduct, so.getSubTree(i));
						}
					} else {
						newProduct = multiply(newProduct, so.getSubTree(i));
					}
				}

				if (rootCount > 1) {
					for (int i = 0; i < so.noOfOperands(); i++) {
						if (so.getSubTree(i).isOperation(Operation.NROOT)) {
							double currentRoot = ((StepOperation) so.getSubTree(i)).getSubTree(1).getValue();
							if (isEqual(commonRoot, 0) || isEqual(commonRoot, currentRoot)) {
								so.getSubTree(i).setColor(tracker.getColorTracker());
							}
						}
					}

					StepExpression result = root(underRoot, commonRoot);
					result.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.PRODUCT_OF_ROOTS, tracker.incColorTracker());

					return multiply(newProduct, result);
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SQUARE_ROOT_MULTIPLIED_BY_ITSELF {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				StepOperation newProduct = new StepOperation(Operation.MULTIPLY);

				boolean[] found = new boolean[so.noOfOperands()];
				for (int i = 0; i < so.noOfOperands(); i++) {
					if (so.getSubTree(i).isSquareRoot()) {
						for (int j = i + 1; j < so.noOfOperands(); j++) {
							if (so.getSubTree(i).equals(so.getSubTree(j))) {
								StepExpression result = ((StepOperation) so.getSubTree(i)).getSubTree(0).deepCopy();

								found[i] = found[j] = true;

								so.getSubTree(i).setColor(tracker.getColorTracker());
								so.getSubTree(j).setColor(tracker.getColorTracker());
								result.setColor(tracker.getColorTracker());

								sb.add(SolutionStepType.SQUARE_ROOT_MULTIPLIED_BY_ITSELF, tracker.incColorTracker());

								newProduct.addSubTree(result);
							}
						}
					}

					if (!found[i]) {
						newProduct.addSubTree(so.getSubTree(i));
					}
				}

				if (newProduct.noOfOperands() == 1) {
					return newProduct.getSubTree(0);
				}

				return newProduct;
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	DOUBLE_MINUS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MINUS)) {
				StepOperation so = (StepOperation) sn;

				if (so.getSubTree(0).isOperation(Operation.MINUS)) {
					StepExpression result = ((StepOperation) so.getSubTree(0)).getSubTree(0);
					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.DOUBLE_MINUS, tracker.incColorTracker());

					return result;
				}
				if (so.getSubTree(0).nonSpecialConstant() && so.getSubTree(0).getValue() < 0) {
					StepExpression result = new StepConstant(so.getValue());
					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.DOUBLE_MINUS, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	DISTRIBUTE_ROOT_OVER_FRACTION {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.NROOT)) {
				StepOperation so = (StepOperation) sn;

				if (so.getSubTree(0).isOperation(Operation.DIVIDE)) {
					StepExpression numerator = root(((StepOperation) so.getSubTree(0)).getSubTree(0), so.getSubTree(1));
					StepExpression denominator = root(((StepOperation) so.getSubTree(0)).getSubTree(1),
							so.getSubTree(1));

					StepExpression result = divide(numerator, denominator);
					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.DISTRIBUTE_ROOT_FRAC, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	RATIONALIZE_DENOMINATOR {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.DIVIDE)) {
				StepOperation so = (StepOperation) sn;

				if (so.getSubTree(1).isOperation(Operation.NROOT)) {
					double root = ((StepOperation) so.getSubTree(1)).getSubTree(1).getValue();

					if (closeToAnInteger(root)) {
						StepExpression toMultiply = root(
								nonTrivialPower(((StepOperation) so.getSubTree(1)).getSubTree(0), root - 1), root);

						toMultiply.setColor(tracker.incColorTracker());

						StepExpression numerator = nonTrivialProduct(so.getSubTree(0), toMultiply);
						StepExpression denominator = multiply(so.getSubTree(1), toMultiply);

						StepExpression result = divide(numerator, denominator);
						sb.add(SolutionStepType.MULTIPLY_NUM_DENOM, toMultiply);

						return result;
					}
				}

				if (so.getSubTree(1).isOperation(Operation.PLUS)) {
					StepOperation sum = (StepOperation) so.getSubTree(1);

					if (sum.noOfOperands() == 2
							&& (sum.getSubTree(0).containsSquareRoot() || sum.getSubTree(1).containsSquareRoot())) {
						StepExpression toMultiply = add(sum.getSubTree(0), sum.getSubTree(1).negate());

						toMultiply.setColor(tracker.incColorTracker());

						StepExpression numerator = nonTrivialProduct(so.getSubTree(0), toMultiply);
						StepExpression denominator = multiply(so.getSubTree(1), toMultiply);

						StepExpression result = divide(numerator, denominator);
						sb.add(SolutionStepType.MULTIPLY_NUM_DENOM, toMultiply);

						return result;
					}
				}

				if (so.getSubTree(1).isOperation(Operation.MULTIPLY)) {
					StepOperation product = (StepOperation) so.getSubTree(1);

					StepExpression irrational = null;

					for (int i = 0; irrational == null && i < product.noOfOperands(); i++) {
						if (product.getSubTree(i).isSquareRoot()) {
							irrational = product.getSubTree(i).deepCopy();
						}
					}

					if (irrational != null) {
						irrational.setColor(tracker.incColorTracker());

						StepExpression numerator = nonTrivialProduct(so.getSubTree(0), irrational);
						StepExpression denominator = multiply(so.getSubTree(1), irrational);

						StepExpression result = divide(numerator, denominator);
						sb.add(SolutionStepType.MULTIPLY_NUM_DENOM, irrational);

						return result;
					}
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	DISTRIBUTE_MINUS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn instanceof StepOperation) {
				StepOperation so = (StepOperation) sn;

				if (so.isOperation(Operation.MULTIPLY) && isEqual(so.getSubTree(0), -1)) {
					if (so.noOfOperands() == 2 && so.getSubTree(1).isOperation(Operation.PLUS)) {
						so = (StepOperation) minus(so.getSubTree(1));
					}
				}

				if (so.isOperation(Operation.MINUS)) {
					if (so.getSubTree(0).isOperation(Operation.PLUS)) {
						StepOperation result = new StepOperation(Operation.PLUS);
						for (int i = 0; i < ((StepOperation) so.getSubTree(0)).noOfOperands(); i++) {
							result.addSubTree(((StepOperation) so.getSubTree(0)).getSubTree(i).negate());
						}

						so.setColor(tracker.getColorTracker());
						result.setColor(tracker.getColorTracker());
						sb.add(SolutionStepType.DISTRIBUTE_MINUS, tracker.incColorTracker());

						return result;
					}
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	FACTOR_SQUARE {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.NROOT)) {
				StepOperation so = (StepOperation) sn;

				StepExpression coefficient = so.getSubTree(0).getIntegerCoefficient();
				StepExpression remainder = so.getSubTree(0).getNonInteger();

				if (closeToAnInteger(coefficient)) {
					long root = Math.round(so.getSubTree(1).getValue());

					long power = StepNode.getIntegerPower(Math.round(coefficient.getValue()));
					long gcd = gcd(root, power);

					if (gcd > 1) {
						StepExpression newValue = power(
								new StepConstant(Math.pow(so.getSubTree(0).getValue(), ((double) 1) / gcd)), gcd);

						so.getSubTree(0).setColor(tracker.getColorTracker());
						newValue.setColor(tracker.incColorTracker());

						StepExpression result = root(newValue, root);
						sb.add(SolutionStepType.REWRITE_AS, so.getSubTree(0), newValue);

						return result;
					}

					long newCoefficient = StepNode.largestNthPower(coefficient.getValue(), so.getSubTree(1).getValue());

					if (!isEqual(newCoefficient, 1)) {
						StepExpression result = multiply(newCoefficient,
								root(multiply(coefficient.getValue() / Math.pow(newCoefficient, root), remainder),
										so.getSubTree(1)));

						so.setColor(tracker.getColorTracker());
						result.setColor(tracker.getColorTracker());

						sb.add(SolutionStepType.FACTOR_SQUARE, tracker.incColorTracker());

						return result;
					}
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	REGROUP_SUMS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.PLUS)) {
				int colorsAtStart = tracker.getColorTracker();

				StepNode tempResult = regroupSums((StepOperation) sn, sb, tracker, false);
				if (colorsAtStart != tracker.getColorTracker()) {
					return tempResult;
				}

				tempResult = regroupSums((StepOperation) sn, sb, tracker, true);
				if (colorsAtStart != tracker.getColorTracker()) {
					return tempResult;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}

		private StepNode regroupSums(StepOperation so, SolutionBuilder sb, RegroupTracker tracker, boolean integer) {
			StepExpression[] coefficients = new StepExpression[so.noOfOperands()];
			StepExpression[] variables = new StepExpression[so.noOfOperands()];
			for (int i = 0; i < so.noOfOperands(); i++) {
				if (integer) {
					coefficients[i] = so.getSubTree(i).getIntegerCoefficient();
					variables[i] = so.getSubTree(i).getNonInteger();
				} else {
					coefficients[i] = so.getSubTree(i).getCoefficient();
					variables[i] = so.getSubTree(i).getVariable();
				}

				if (coefficients[i] == null) {
					coefficients[i] = new StepConstant(1);
				}
				if (variables[i] == null) {
					variables[i] = new StepConstant(1);
				}
			}

			List<StepExpression> constantList = new ArrayList<StepExpression>();
			double constantSum = 0;
			for (int i = 0; i < so.noOfOperands(); i++) {
				if (coefficients[i].nonSpecialConstant() && isEqual(variables[i], 1)) {
					constantList.add(coefficients[i]);
					constantSum += coefficients[i].getValue();
					coefficients[i] = new StepConstant(0);
				}
			}

			for (int i = 0; i < so.noOfOperands(); i++) {
				if ((integer || !variables[i].isConstant()) && !isEqual(coefficients[i], 0)) {
					boolean foundCommon = false;
					for (int j = i + 1; j < so.noOfOperands(); j++) {
						if (!isEqual(coefficients[j], 0) && !isEqual(variables[i], 1)
								&& variables[i].equals(variables[j])) {
							foundCommon = true;
							so.getSubTree(j).setColor(tracker.getColorTracker());
							coefficients[i] = add(coefficients[i], coefficients[j]);
							coefficients[j] = new StepConstant(0);
						}
					}
					if (foundCommon) {
						so.getSubTree(i).setColor(tracker.getColorTracker());
						coefficients[i].setColor(tracker.getColorTracker());
						variables[i].setColor(tracker.getColorTracker());
						sb.add(SolutionStepType.COLLECT_LIKE_TERMS, variables[i]);
						tracker.incColorTracker();
					}
				}
			}

			StepOperation newSum = new StepOperation(Operation.PLUS);

			for (int i = 0; i < so.noOfOperands(); i++) {
				if (!coefficients[i].equals(new StepConstant(0)) && !variables[i].equals(new StepConstant(0))) {
					if (coefficients[i].nonSpecialConstant() && isEqual(coefficients[i], 1)) {
						newSum.addSubTree(variables[i]);
					} else if (variables[i].nonSpecialConstant() && isEqual(variables[i], 1)) {
						newSum.addSubTree(coefficients[i]);
					} else if (coefficients[i].nonSpecialConstant() && isEqual(coefficients[i], -1)) {
						newSum.addSubTree(minus(variables[i]));
					} else {
						newSum.addSubTree(multiply(coefficients[i], variables[i]));
					}
				}
			}

			StepExpression newConstants = new StepConstant(constantSum);
			if (constantList.size() > 1) {
				for (int i = 0; i < constantList.size(); i++) {
					constantList.get(i).setColor(tracker.getColorTracker());
				}
				sb.add(SolutionStepType.ADD_CONSTANTS, tracker.getColorTracker());
				newConstants.setColor(tracker.getColorTracker());
				tracker.incColorTracker();
			}

			if (isEqual(constantSum, 0) && constantList.size() == 1) {
				constantList.get(0).setColor(tracker.getColorTracker());
				sb.add(SolutionStepType.ZERO_IN_ADDITION, tracker.incColorTracker());
			}

			if (!isEqual(constantSum, 0)) {
				newSum.addSubTree(newConstants);
			}

			if (newSum.noOfOperands() == 0) {
				return new StepConstant(0);
			} else if (newSum.noOfOperands() == 1) {
				return newSum.getSubTree(0);
			}

			return newSum;
		}
	},

	EXPAND_FRACTIONS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.PLUS)) {
				StepOperation so = (StepOperation) sn;

				StepOperation newSum = new StepOperation(Operation.PLUS);

				long newDenominator = 1;

				for (int i = 0; i < so.noOfOperands(); i++) {
					long currentDenominator = StepExpression.getConstantDenominator(so.getSubTree(i));
					if (currentDenominator != 0) {
						newDenominator = lcm(newDenominator, currentDenominator);
					}
				}

				if (newDenominator != 1) {
					boolean wasChanged = false;

					for (int i = 0; i < so.noOfOperands(); i++) {
						long currentDenominator = StepExpression.getConstantDenominator(so.getSubTree(i));
						if (currentDenominator != 0 && currentDenominator != newDenominator) {
							wasChanged = true;

							StepExpression newFraction = divide(
									nonTrivialProduct(new StepConstant(((double) newDenominator) / currentDenominator),
											StepExpression.getNumerator(so.getSubTree(i))),
									newDenominator);

							newFraction.setColor(tracker.getColorTracker());
							so.getSubTree(i).setColor(tracker.getColorTracker());

							newSum.addSubTree(newFraction);
						} else {
							newSum.addSubTree(so.getSubTree(i));
						}
					}

					if (wasChanged) {
						StepConstant denominatorNode = new StepConstant(newDenominator);
						denominatorNode.setColor(tracker.incColorTracker());
						sb.add(SolutionStepType.EXPAND_FRACTIONS, denominatorNode);

						return newSum;
					}
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	ADD_NUMERATORS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.PLUS)) {
				StepOperation so = (StepOperation) sn;

				StepExpression remainder = null;
				StepExpression newNumerator = null;
				long newDenominator = 0;

				List<StepExpression> fractions = new ArrayList<StepExpression>();
				for (int i = 0; i < so.noOfOperands(); i++) {
					StepExpression currentNumerator = StepExpression.getNumerator(so.getSubTree(i));
					long currentDenominator = StepExpression.getConstantDenominator(so.getSubTree(i));

					if (newDenominator == 0 && currentDenominator != 0 && currentDenominator != 1) {
						newDenominator = currentDenominator;
					}

					if (currentDenominator != 0 && currentDenominator == newDenominator) {
						newNumerator = add(newNumerator, currentNumerator);
						fractions.add(so.getSubTree(i));
					} else {
						remainder = add(remainder, so.getSubTree(i));
					}
				}

				if (fractions.size() > 1) {
					for (int i = 0; i < fractions.size(); i++) {
						fractions.get(i).setColor(tracker.getColorTracker());
					}

					StepExpression result = divide(newNumerator, newDenominator);
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.ADD_NUMERATORS, tracker.incColorTracker());
					return add(remainder, result);
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SIMPLIFY_FRACTIONS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn instanceof StepOperation) {
				StepOperation so = (StepOperation) sn;

				if (so.isOperation(Operation.DIVIDE) && isEqual(so.getSubTree(1), 1)) {
					so.getSubTree(1).setColor(tracker.incColorTracker());
					sb.add(SolutionStepType.DIVIDE_BY_ONE);
					return so.getSubTree(0).deepCopy();
				}

				if (so.isOperation(Operation.DIVIDE) && isEqual(so.getSubTree(1), -1)) {
					so.getSubTree(1).setColor(tracker.incColorTracker());
					sb.add(SolutionStepType.DIVIDE_BY_NEGATVE_ONE);
					return minus(so.getSubTree(0).deepCopy());
				}

				if (so.isOperation(Operation.MULTIPLY) || so.isOperation(Operation.DIVIDE)) {
					List<StepExpression> bases = new ArrayList<StepExpression>();
					List<StepExpression> exponents = new ArrayList<StepExpression>();

					int colorsAtStart = tracker.getColorTracker();

					StepExpression.getBasesAndExponents(so, null, bases, exponents);

					for (int i = 0; i < bases.size(); i++) {
						for (int j = i + 1; j < bases.size(); j++) {
							if ((exponents.get(i).getValue() * exponents.get(j).getValue()) < 0
									&& bases.get(i).equals(bases.get(j)) && !isEqual(bases.get(i), 1)) {
								bases.get(i).setColor(tracker.getColorTracker());
								bases.get(j).setColor(tracker.getColorTracker());

								double min = Math.min(Math.abs(exponents.get(i).getValue()),
										Math.abs(exponents.get(j).getValue()));

								exponents.get(i).setColor(tracker.getColorTracker());
								exponents.get(j).setColor(tracker.getColorTracker());

								double newExponent1 = exponents.get(i).getValue() > 0
										? exponents.get(i).getValue() - min
										: exponents.get(i).getValue() + min;
								double newExponent2 = exponents.get(j).getValue() > 0
										? exponents.get(j).getValue() - min
										: exponents.get(j).getValue() + min;

								exponents.set(i, new StepConstant(newExponent1));
								exponents.set(j, new StepConstant(newExponent2));

								exponents.get(i).setColor(tracker.getColorTracker());
								exponents.get(j).setColor(tracker.getColorTracker());

								StepExpression toCancel = nonTrivialPower(bases.get(i), min);
								toCancel.setColor(tracker.incColorTracker());
								sb.add(SolutionStepType.CANCEL_FRACTION, toCancel);

								break;
							}
							if (isEqual(exponents.get(i), 1) && isEqual(exponents.get(j), -1)
									&& closeToAnInteger(bases.get(i)) && closeToAnInteger(bases.get(j))) {
								long gcd = gcd(bases.get(i), bases.get(j));
								if (gcd > 1) {
									bases.get(i).setColor(tracker.getColorTracker());
									bases.get(j).setColor(tracker.getColorTracker());

									bases.set(i, new StepConstant(bases.get(i).getValue() / gcd));
									bases.set(j, new StepConstant(bases.get(j).getValue() / gcd));

									bases.get(i).setColor(tracker.getColorTracker());
									bases.get(j).setColor(tracker.getColorTracker());

									StepExpression toCancel = new StepConstant(gcd);
									toCancel.setColor(tracker.incColorTracker());
									sb.add(SolutionStepType.CANCEL_FRACTION, toCancel);

									break;
								}

								if (isEqual(bases.get(i).getValue() % bases.get(j).getValue(), 0)) {
									bases.set(i, new StepConstant(bases.get(i).getValue() / bases.get(j).getValue()));
									bases.set(j, new StepConstant(1));
								}
							}

							if (isEqual(exponents.get(i), 1) && isEqual(exponents.get(j), -1)) {
								StepExpression numByDenom = StepExpression.tryToDivide(bases.get(i), bases.get(j));

								if (numByDenom != null && !bases.get(j).isConstant()) {
									bases.get(i).setColor(tracker.getColorTracker());
									bases.get(j).setColor(tracker.getColorTracker());
									numByDenom.setColor(tracker.getColorTracker());

									sb.add(SolutionStepType.POLYNOMIAL_DIVISION, bases.get(i), bases.get(j),
											numByDenom);

									bases.set(i, numByDenom);
									bases.set(j, new StepConstant(1));
									tracker.incColorTracker();

									continue;
								}

								StepExpression denomByNum = StepExpression.tryToDivide(bases.get(j), bases.get(i));

								if (denomByNum != null && !bases.get(i).isConstant()) {
									bases.get(i).setColor(tracker.getColorTracker());
									bases.get(j).setColor(tracker.getColorTracker());
									denomByNum.setColor(tracker.getColorTracker());

									sb.add(SolutionStepType.POLYNOMIAL_DIVISION, bases.get(j), bases.get(i),
											denomByNum);

									bases.set(i, new StepConstant(1));
									bases.set(j, denomByNum);
									tracker.incColorTracker();
								}
							}
						}
					}

					if (tracker.getColorTracker() == colorsAtStart) {
						return so;
					}

					StepExpression newFraction = null;
					for (int i = 0; i < bases.size(); i++) {
						if (!isEqual(exponents.get(i), 0) && !isEqual(bases.get(i), 1)) {
							newFraction = StepExpression.makeFraction(newFraction, bases.get(i), exponents.get(i));
						}
					}

					return newFraction == null ? new StepConstant(1) : newFraction;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	COMMON_FRACTION {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				List<StepExpression> bases = new ArrayList<StepExpression>();
				List<StepExpression> exponents = new ArrayList<StepExpression>();

				StepExpression.getBasesAndExponents(so, null, bases, exponents);

				StepExpression newFraction = null;

				for (int i = 0; i < bases.size(); i++) {
					newFraction = StepExpression.makeFraction(newFraction, bases.get(i), exponents.get(i));
				}

				if (newFraction.isOperation(Operation.DIVIDE)) {
					so.setColor(tracker.getColorTracker());
					newFraction.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.COMMON_FRACTION, tracker.incColorTracker());

					return newFraction;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}

	},

	MULTIPLY_NEGATIVES {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY) || sn.isOperation(Operation.MINUS)
					&& ((StepOperation) sn).getSubTree(0).isOperation(Operation.MULTIPLY)) {
				StepOperation so;

				if (sn.isOperation(Operation.MINUS)) {
					so = (StepOperation) ((StepOperation) sn).getSubTree(0);
				} else {
					so = (StepOperation) sn;
				}

				int negativeCount = 0;
				StepExpression result = null;

				for (StepExpression operand : so) {
					if (isEqual(operand, -1)) {
						negativeCount++;
					} else if (operand.isNegative()) {
						negativeCount++;
						result = multiply(result, operand.negate());
					} else {
						result = multiply(result, operand);
					}
				}

				if (negativeCount == 0) {
					return StepStrategies.iterateThrough(this, sn, sb, tracker);
				}

				if (negativeCount == 1 && so.getSubTree(0).isNegative()) {
					return StepStrategies.iterateThrough(this, sn, sb, tracker);
				}

				if (sn.isOperation(Operation.MINUS)) {
					negativeCount++;
				}

				if (result == null) {
					result = new StepConstant(1);
				}

				sn.setColor(tracker.getColorTracker());
				result.setColor(tracker.getColorTracker());

				if (negativeCount % 2 == 1) {
					sb.add(SolutionStepType.ODD_NUMBER_OF_NEGATIVES, tracker.incColorTracker());
					return minus(result);
				}

				sb.add(SolutionStepType.EVEN_NUMBER_OF_NEGATIVES, tracker.incColorTracker());
				return result;
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	REGROUP_PRODUCTS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				int colorsAtStart = tracker.getColorTracker();

				StepOperation so = (StepOperation) sn;

				List<StepExpression> bases = new ArrayList<StepExpression>();
				List<StepExpression> exponents = new ArrayList<StepExpression>();

				StepExpression.getBasesAndExponents(so, null, bases, exponents);

				List<StepExpression> constantList = new ArrayList<StepExpression>();
				double constantValue = 1;
				for (int i = 0; i < bases.size(); i++) {
					if (bases.get(i).nonSpecialConstant() && isEqual(exponents.get(i), 1)) {
						constantList.add(bases.get(i));
						constantValue *= bases.get(i).getValue();

						exponents.set(i, new StepConstant(0));
					}
				}

				if (isEqual(constantValue, 0)) {
					so.setColor(tracker.getColorTracker());
					StepExpression result = new StepConstant(0);
					result.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.MULTIPLIED_BY_ZERO, tracker.incColorTracker());
					return result;
				}

				if (constantList.size() == 1 && isEqual(constantList.get(0), 1)) {
					constantList.get(0).setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.MULTIPLIED_BY_ONE, tracker.incColorTracker());
				}

				for (int i = 0; i < bases.size(); i++) {
					if (!isEqual(exponents.get(i), 0)) {
						boolean foundCommon = false;
						for (int j = i + 1; j < bases.size(); j++) {
							if (!isEqual(exponents.get(j), 0) && bases.get(i).equals(bases.get(j))) {
								foundCommon = true;
								bases.get(j).setColor(tracker.getColorTracker());

								exponents.set(i, add(exponents.get(i), exponents.get(j)));
								exponents.set(j, new StepConstant(0));
							}
						}
						if (foundCommon) {
							bases.get(i).setColor(tracker.getColorTracker());
							exponents.get(i).setColor(tracker.incColorTracker());

							sb.add(SolutionStepType.REGROUP_PRODUCTS, bases.get(i));
						}
					}
				}

				StepExpression newProduct = null;

				for (int i = 0; i < bases.size(); i++) {
					if (!isEqual(exponents.get(i), 0) && !isEqual(bases.get(i), 1)) {
						newProduct = StepExpression.makeFraction(newProduct, bases.get(i), exponents.get(i));
					}
				}

				StepExpression newConstant;
				newConstant = new StepConstant(Math.abs(constantValue));
				if (constantList.size() > 1) {
					for (int i = 0; i < constantList.size(); i++) {
						constantList.get(i).setColor(tracker.getColorTracker());
					}
					sb.add(SolutionStepType.MULTIPLY_CONSTANTS, tracker.getColorTracker());
					if (newProduct == null) {
						newConstant = new StepConstant(constantValue);
						newConstant.setColor(tracker.incColorTracker());
						return newConstant;
					}
					newConstant.setColor(tracker.incColorTracker());
				}

				newProduct = StepExpression.makeFraction(newConstant, newProduct, new StepConstant(1));

				if (tracker.getColorTracker() > colorsAtStart) {
					if (constantValue < 0) {
						return minus(newProduct);
					}
					return newProduct;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	DISTRIBUTE_POWER_OVER_PRODUCT {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.POWER)) {
				StepOperation so = (StepOperation) sn;

				if (so.getSubTree(0).isOperation(Operation.MULTIPLY)) {
					StepOperation result = new StepOperation(Operation.MULTIPLY);

					so.getSubTree(1).setColor(tracker.incColorTracker());
					for (int i = 0; i < ((StepOperation) so.getSubTree(0)).noOfOperands(); i++) {
						((StepOperation) so.getSubTree(0)).getSubTree(i).setColor(tracker.incColorTracker());
						result.addSubTree(power(((StepOperation) so.getSubTree(0)).getSubTree(i), so.getSubTree(1)));
					}

					sb.add(SolutionStepType.DISTRIBUTE_POWER_OVER_PRODUCT);

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	POWER_OF_NEGATIVE {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.POWER)) {
				StepOperation so = (StepOperation) sn;

				if (so.getSubTree(0).isNegative()) {
					if (isEven(so.getSubTree(1))) {
						StepExpression result = power(so.getSubTree(0).negate(), so.getSubTree(1));

						so.setColor(tracker.getColorTracker());
						result.setColor(tracker.getColorTracker());
						sb.add(SolutionStepType.EVEN_POWER_NEGATIVE, tracker.incColorTracker());

						return result;
					} else if (isOdd(so.getSubTree(1))) {
						StepExpression result = power(so.getSubTree(0).negate(), so.getSubTree(1)).negate();

						so.setColor(tracker.getColorTracker());
						result.setColor(tracker.getColorTracker());
						sb.add(SolutionStepType.ODD_POWER_NEGATIVE, tracker.incColorTracker());

						return result;
					}
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SIMPLIFY_POWERS_AND_ROOTS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn instanceof StepOperation) {
				StepOperation so = (StepOperation) sn;

				if ((so.isOperation(Operation.POWER) && so.getSubTree(0).isOperation(Operation.NROOT))
						|| (so.isOperation(Operation.NROOT) && so.getSubTree(0).isOperation(Operation.POWER))) {
					StepExpression exponent1 = so.getSubTree(1);
					StepExpression exponent2 = ((StepOperation) so.getSubTree(0)).getSubTree(1);

					if (closeToAnInteger(exponent1) && closeToAnInteger(exponent2)) {
						long gcd = gcd(Math.round(exponent1.getValue()), Math.round(exponent2.getValue()));

						if (gcd > 1) {
							exponent1 = isEqual(exponent1, gcd) ? null : new StepConstant(exponent1.getValue() / gcd);
							exponent2 = isEqual(exponent2, gcd) ? null : new StepConstant(exponent2.getValue() / gcd);

							StepConstant gcdConstant = new StepConstant(gcd);
							gcdConstant.setColor(tracker.getColorTracker());

							StepExpression argument = ((StepOperation) so.getSubTree(0)).getSubTree(0);

							StepExpression result;
							if (so.isOperation(Operation.NROOT) && so.getSubTree(0).isOperation(Operation.POWER)) {
								if (isEven(gcd) && (exponent2 == null || !isEven(exponent2.getValue()))) {
									if (argument.nonSpecialConstant()) {
										result = root(power(new StepConstant(Math.abs(argument.getValue())), exponent2),
												exponent1);
									} else {
										result = root(power(abs(argument), exponent2), exponent1);
									}
									sb.add(SolutionStepType.REDUCE_ROOT_AND_POWER_EVEN, gcdConstant);
								} else {
									result = root(power(argument, exponent2), exponent1);
									sb.add(SolutionStepType.REDUCE_ROOT_AND_POWER, gcdConstant);
								}
							} else {
								result = power(root(argument, exponent2), exponent1);
								sb.add(SolutionStepType.REDUCE_ROOT_AND_POWER, gcdConstant);
							}

							so.setColor(tracker.getColorTracker());
							result.setColor(tracker.incColorTracker());
							return result;
						}
					}
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SIMPLE_POWERS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.POWER)) {
				StepOperation so = (StepOperation) sn;

				if (so.getSubTree(0).isOperation(Operation.POWER)) {
					StepExpression result = power(((StepOperation) so.getSubTree(0)).getSubTree(0),
							so.getSubTree(1).getValue() * ((StepOperation) so.getSubTree(0)).getSubTree(1).getValue());

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.POWER_OF_POWER, tracker.incColorTracker());

					return result;
				}

				if (closeToAnInteger(so.getSubTree(0)) && closeToAnInteger(so.getSubTree(1))) {
					StepExpression result = new StepConstant(
							Math.pow(so.getSubTree(0).getValue(), so.getSubTree(1).getValue()));

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.EVALUATE_POWER, tracker.incColorTracker());

					return result;
				}
				if (isEqual(so.getSubTree(1), 0)) {
					StepExpression result = new StepConstant(1);

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.ZEROTH_POWER, tracker.incColorTracker());

					return result;
				}
				if (isEqual(so.getSubTree(1), 1)) {
					StepExpression result = so.getSubTree(0);

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.FIRST_POWER, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SIMPLE_ROOTS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.NROOT)) {
				StepOperation so = (StepOperation) sn;

				if (so.getSubTree(0).isOperation(Operation.NROOT)) {
					StepExpression result = root(((StepOperation) so.getSubTree(0)).getSubTree(0),
							so.getSubTree(1).getValue() * ((StepOperation) so.getSubTree(0)).getSubTree(1).getValue());

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.ROOT_OF_ROOT, tracker.incColorTracker());

					return result;
				}

				if (isEqual(so.getSubTree(0), 1)) {
					StepExpression result = new StepConstant(1);

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.ROOT_OF_ONE, tracker.incColorTracker());

					return result;
				}

				if (isEqual(so.getSubTree(1), 1)) {
					StepExpression result = so.getSubTree(0);

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.FIRST_ROOT, tracker.incColorTracker());

					return result;
				}

				if (isOdd(so.getSubTree(1).getValue()) && so.getSubTree(0).isNegative()) {
					StepExpression result = minus(root(so.getSubTree(0).negate(), so.getSubTree(1)));

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.ODD_ROOT_OF_NEGATIVE, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	CALCULATE_INVERSE_TRIGO {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn instanceof StepOperation && ((StepOperation) sn).isInverseTrigonometric()) {
				StepOperation so = (StepOperation) sn;

				StepExpression value = StepExpression.inverseTrigoLookup(so);
				if (value != null) {
					so.setColor(tracker.getColorTracker());
					value.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.EVALUATE_INVERSE_TRIGO, tracker.incColorTracker());
					return value;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	ADD_FRACTIONS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			RegroupSteps[] fractionAddition = new RegroupSteps[] { EXPAND_FRACTIONS, ADD_NUMERATORS, REGROUP_PRODUCTS,
					REGROUP_SUMS, SIMPLIFY_FRACTIONS };

			SolutionBuilder tempSteps = new SolutionBuilder();
			RegroupTracker tempTracker = new RegroupTracker();
			StepNode tempTree = sn.deepCopy();

			tempTree = EXPAND_FRACTIONS.apply(tempTree, tempSteps, tempTracker);
			ADD_NUMERATORS.apply(tempTree, tempSteps, tempTracker);

			if (tempTracker.wasChanged()) {
				SolutionBuilder additionSteps = new SolutionBuilder();

				StepNode newSn = StepStrategies.implementStrategy(sn, additionSteps, fractionAddition);

				if (sb != null) {
					sb.add(SolutionStepType.ADD_FRACTIONS);
					sb.levelDown();
					sb.addAll(additionSteps.getSteps());
					sb.levelUp();
				}

				tracker.incColorTracker();

				return newSn;
			}

			return sn;
		}
	},

	RATIONALIZE_DENOMINATORS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			SimplificationStepGenerator[] denominatorRationalization = new SimplificationStepGenerator[] {
					RATIONALIZE_DENOMINATOR, SQUARE_ROOT_MULTIPLIED_BY_ITSELF, EXPAND_ROOT, COMMON_ROOT, SIMPLE_POWERS,
					SIMPLE_ROOTS, REGROUP_PRODUCTS, REGROUP_SUMS, ExpandSteps.EXPAND_PRODUCTS, FACTOR_SQUARE,
					SIMPLIFY_POWERS_AND_ROOTS };

			SolutionBuilder tempSteps = new SolutionBuilder();
			RegroupTracker tempTracker = new RegroupTracker();
			StepNode tempTree = sn.deepCopy();

			RATIONALIZE_DENOMINATOR.apply(tempTree, tempSteps, tempTracker);

			if (tempTracker.wasChanged()) {
				SolutionBuilder rationalizationSteps = new SolutionBuilder();

				RegroupTracker denominatorTracker = new RegroupTracker(true, false);

				StepNode result = sn;
				String old, current = null;

				do {
					result = StepStrategies.implementStrategy(result, rationalizationSteps, denominatorRationalization,
							denominatorTracker);
					old = current;
					current = result.toString();
				} while (!current.equals(old));

				if (sb != null) {
					sb.add(SolutionStepType.RATIONALIZE_DENOMINATOR);
					sb.levelDown();
					sb.addAll(rationalizationSteps.getSteps());
					sb.levelUp();
				}

				tracker.incColorTracker();
				return result;
			}

			return sn;
		}
	};
}
