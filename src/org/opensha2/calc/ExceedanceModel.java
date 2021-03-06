package org.opensha2.calc;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.min;
import static org.opensha2.gmm.Imt.PGA;
import static org.opensha2.gmm.Imt.PGV;
import static org.opensha2.gmm.Imt.SA0P75;

import org.opensha2.data.XY_Point;
import org.opensha2.data.XY_Sequence;
import org.opensha2.gmm.Imt;

/**
 * Uncertainty models govern how the values of a complementary cumulative normal
 * distribution (or probability of exceedence) are computed given a mean, μ,
 * standard deviation, σ, and other possibly relevant arguments.
 * 
 * <p>Each model implements methods that compute the probability of exceeding a
 * single value or a {@link XY_Sequence} of values. Some arguments are only used
 * by some models; for example, {@link #NONE} ignores σ, but it must be supplied
 * for consistency. See individual models for details.</p>
 * 
 * <p>Internally, models use a high precision approximation of the Gauss error
 * function (see Abramowitz and Stegun 7.1.26) when computing exceedances.</p>
 * 
 * @author Peter Powers
 */
public enum ExceedanceModel {

	/**
	 * No uncertainty. Any {@code σ} supplied to methods is ignored yielding a
	 * complementary unit step function for a range of values spanning μ.
	 * 
	 * <p>Model ignores {@code σ}, truncation level,{@code n}, and {@code imt}
	 * .</p>
	 */
	NONE {
		@Override double exceedance(double μ, double σ, double n, Imt imt, double value) {
			return stepFn(μ, value);
		}

		@Override XY_Sequence exceedance(double μ, double σ, double n, Imt imt, XY_Sequence sequence) {
			for (XY_Point p : sequence) {
				p.set(stepFn(μ, p.x()));
			}
			return sequence;
		}
	},

	/**
	 * No truncation.
	 * 
	 * <p>Model ignores truncation level, {@code n}, and {@code imt}.</p>
	 */
	TRUNCATION_OFF {
		@Override double exceedance(double μ, double σ, double n, Imt imt, double value) {
			return boundedCcdFn(μ, σ, value, 0.0, 1.0);
		}

		@Override XY_Sequence exceedance(double μ, double σ, double n, Imt imt, XY_Sequence sequence) {
			return boundedCcdFn(μ, σ, sequence, 0.0, 1.0);
		}
	},

	/**
	 * Upper truncation only at {@code μ + σ * n}.
	 * 
	 * <p>Model ignores {@code imt}.</p>
	 */
	TRUNCATION_UPPER_ONLY {
		@Override double exceedance(double μ, double σ, double n, Imt imt, double value) {
			return boundedCcdFn(μ, σ, value, prob(μ, σ, n), 1.0);
		}

		@Override XY_Sequence exceedance(double μ, double σ, double n, Imt imt, XY_Sequence sequence) {
			return boundedCcdFn(μ, σ, sequence, prob(μ, σ, n), 1.0);
		}
	},

	/**
	 * Upper and lower truncation at {@code μ ± σ * n}.
	 * 
	 * <p>Model ignores {@code imt}.</p>
	 */
	TRUNCATION_LOWER_UPPER {
		@Override double exceedance(double μ, double σ, double n, Imt imt, double value) {
			double pHi = prob(μ, σ, n);
			return boundedCcdFn(μ, σ, value, pHi, 1.0 - pHi);
		}

		@Override XY_Sequence exceedance(double μ, double σ, double n, Imt imt, XY_Sequence sequence) {
			double pHi = prob(μ, σ, n);
			return boundedCcdFn(μ, σ, sequence, pHi, 1.0 - pHi);
		}
	},

	/*
	 * This is messy for now; TODO need to figure out the best way to pass in
	 * fixed sigmas. The peer models below simply set a value internally as
	 * dicated by the test cases that use htese models.
	 */
	PEER_MIXTURE_REFERENCE {
		@Override double exceedance(double μ, double σ, double n, Imt imt, double value) {
			return boundedCcdFn(μ, 0.65, value, 0.0, 1.0);
		}

		@Override XY_Sequence exceedance(double μ, double σ, double n, Imt imt, XY_Sequence sequence) {
			return boundedCcdFn(μ, 0.65, sequence, 0.0, 1.0);
		}
	},
	
	/**
	 * Model accomodates the heavy tails observed in earthquake data that are
	 * not well matched by a purely normal distribution at high ε by combining
	 * two distributions (with 50% weight each) created using modulated σ-values
	 * (0.8σ and 1.2σ). Model does not impose any truncation.
	 * 
	 * <p>Model ignores truncation level, {@code n}, and {@code imt}.</p>
	 */
	PEER_MIXTURE_MODEL {
		@Override double exceedance(double μ, double σ, double n, Imt imt, double value) {
			σ = 0.65;
			double p1 = boundedCcdFn(μ, σ * 0.8, value, 0.0, 1.0);
			double p2 = boundedCcdFn(μ, σ * 1.2, value, 0.0, 1.0);
			return (p1 + p2) / 2.0;
		}

		@Override XY_Sequence exceedance(double μ, double σ, double n, Imt imt, XY_Sequence sequence) {
			for (XY_Point p : sequence) {
				p.set(exceedance(μ, σ, n, imt, p.x()));
			}
			return sequence;
		}
	},

	/**
	 * Model provides {@link Imt}-dependent maxima end exists to support
	 * 'clamps' on ground motions that have historically been applied in the
	 * CEUS NSHM due to sometimes unreasonably high ground motions implied by
	 * {@code μ + 3σ}. Model imposes one-sided (upper) truncation at
	 * {@code μ + nσ} if clamp is not exceeded.
	 */
	NSHM_CEUS_MAX_INTENSITY {
		@Override double exceedance(double μ, double σ, double n, Imt imt, double value) {
			double pHi = prob(μ, σ, n, log(maxValue(imt)));
			return boundedCcdFn(μ, σ, value, pHi, 1.0);
		}

		@Override XY_Sequence exceedance(double μ, double σ, double n, Imt imt, XY_Sequence sequence) {
			double pHi = prob(μ, σ, n, log(maxValue(imt)));
			return boundedCcdFn(μ, σ, sequence, pHi, 1.0);
		}

		private double maxValue(Imt imt) {
			/*
			 * Clamping/limiting is turned off at and above 0.75 sec.
			 * 
			 * TODO few CEUS Gmms support PGV; only Atkinson 06p and 08p.
			 * Revisit as it may just be more appropriate to throw a UOE.
			 */
			if (imt.isSA()) return imt.ordinal() < SA0P75.ordinal() ? 6.0 : Double.MAX_VALUE;
			if (imt == PGA) return 3.0;
			if (imt == PGV) return 400.0;
			throw new UnsupportedOperationException();
		}

	};

	/**
	 * Compute the probability of exceeding a {@code value}.
	 * 
	 * @param μ mean
	 * @param σ standard deviation
	 * @param n truncation level in units of {@code σ} (truncation = n * σ)
	 * @param imt intenisty measure type (only used by
	 *        {@link #NSHM_CEUS_MAX_INTENSITY}
	 * @param value to compute exceedance for
	 */
	abstract double exceedance(double μ, double σ, double n, Imt imt, double value);

	/**
	 * Compute the probability of exceeding a sequence of x-values.
	 * 
	 * @param μ mean
	 * @param σ standard deviation
	 * @param n truncation level in units of {@code σ} (truncation = n * σ)
	 * @param imt intenisty measure type (only used by
	 *        {@link #NSHM_CEUS_MAX_INTENSITY}
	 * @param sequence the x-values of which to compute exceedance for
	 * @return the supplied {@code sequence}
	 */
	abstract XY_Sequence exceedance(double μ, double σ, double n, Imt imt, XY_Sequence sequence);

	private static final double SQRT_2 = Math.sqrt(2);

	/*
	 * Step function.
	 */
	private static double stepFn(double μ, double value) {
		return value < μ ? 1.0 : 0.0;
	}

	/*
	 * Complementary cumulative distribution. Compute the probability of
	 * exceeding the supplied value in a normal distribution assuming no
	 * truncation.
	 */
	private static double ccdFn(double μ, double σ, double value) {
		return (1.0 + erf((μ - value) / (σ * SQRT_2))) * 0.5;
	}

	/*
	 * Bounded complementary cumulative distribution. Compute the probability
	 * that a value will be exceeded, subject to upper and lower probability
	 * limits.
	 */
	private static double boundedCcdFn(double μ, double σ, double value, double pHi, double pLo) {
		double p = ccdFn(μ, σ, value);
		return probBoundsCheck((p - pHi) / (pLo - pHi));
	}

	/*
	 * Bounded complementary cumulative distribution. Compute the probabilities
	 * that the x-values in {@code values} will be exceeded, subject to upper
	 * and lower probability limits. Return the supplied {@code XY_Sequence}
	 * populated with probabilities.
	 */
	private static XY_Sequence boundedCcdFn(double μ, double σ, XY_Sequence sequence, double pHi,
			double pLo) {
		for (XY_Point p : sequence) {
			p.set(boundedCcdFn(μ, σ, p.x(), pHi, pLo));
		}
		return sequence;
	}

	/*
	 * TODO does this exist due to double precission errors possibly pushing
	 * probabilities above 1 or below 0 ?? Run a test sometime to determine if P
	 * EVER ventures outside [0, 1]
	 */
	private static double probBoundsCheck(double P) {
		return (P < 0.0) ? 0.0 : (P > 1.0) ? 1.0 : P;
	}

	/*
	 * Compute ccd value at μ + nσ.
	 */
	private static double prob(double μ, double σ, double n) {
		return ccdFn(μ, σ, μ + n * σ);
	}

	/*
	 * Compute ccd value at min(μ + nσ, max).
	 */
	private static double prob(double μ, double σ, double n, double max) {
		return ccdFn(μ, σ, min(μ + n * σ, max));
	}

	/*
	 * Abramowitz and Stegun 7.1.26 implementation. This erf(x) approximation is
	 * valid for x ≥ 0. Because erf(x) is an odd function, erf(x) = −erf(−x).
	 */
	private static double erf(double x) {
		return x < 0.0 ? -erfBase(-x) : erfBase(x);
	}

	private static final double P = 0.3275911;
	private static final double A1 = 0.254829592;
	private static final double A2 = -0.284496736;
	private static final double A3 = 1.421413741;
	private static final double A4 = -1.453152027;
	private static final double A5 = 1.061405429;

	private static double erfBase(double x) {
		double t = 1 / (1 + P * x);
		return 1 - (A1 * t +
			A2 * t * t +
			A3 * t * t * t +
			A4 * t * t * t * t +
			A5 * t * t * t * t * t) * exp(-x * x);
	}

}
