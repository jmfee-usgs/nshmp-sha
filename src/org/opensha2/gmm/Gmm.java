package org.opensha2.gmm;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.opensha2.gmm.CeusMb.*;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * {@link GroundMotionModel} (Gmm) identifiers. Use these to generate
 * {@link Imt}-specific instances via {@link Gmm#instance(Imt)}. Single or
 * corporate authored models are identified as NAME_YR_FLAVOR; multi-author
 * models as INITIALS_YR_FLAVOR. FLAVOR is only used for those models with
 * region specific implementations or other variants.
 * 
 * @author Peter Powers
 */
public enum Gmm {

	// TODO 2014 CEUS clamps (see recent emails with harmsen)
	
	// TODO implement AB03 taper developed by SH; gms at 2s and 3s are much too
	// high at large distances

	// TODO do deep GMMs saturate at 7.8 ??? doublecheck

	// TODO check if AtkinsonMacias using BooreAtkin siteAmp to get non-rock
	// site response

	// TODO most CEUS Gmm's have 0.4s coeffs that were linearly interpolated for
	// special NRC project; consider removing them??

	// TODO AB06 has PGV clamp of 460m/s; is this correct? or specified anywhere?

	// TODO Port Gmm grid optimization tables

	// TODO Ensure Atkinson sfac/gfac is implemented correctly
	// TODO amean11 (fortran) has wrong median clamp values and short period
	// ranges

	// TODO is Atkinson Macias ok? finished?
	// TODO is there a citation for Atkinson distance decay
	// mean = mean - 0.3 + 0.15(log(rJB)) (ln or log10 ??)

	// TODO ensure table lookups are using correct distance metric, some are
	// rRup and some are rJB

	// TODO check Fortran minimums (this note may have been written just
	// regarding Gmm table lookups, Atkinson in particular)
	// hazgrid A08' minR=1.8km; P11 minR = 1km; others?
	// hazfx all (tables?) have minR = 0.11km

	// TODO z1p0 in CY08 - this is now always km, CY08 needs updating (from m)

	// TODO Verify that Campbell03 imposes max(dtor,5); he does require rRup;
	// why is depth constrained as such in hazgrid? As with somerville, no depth is
	// imposed in hazFX - make sure 0.01 as PGA is handled corectly; may require
	// change to period = 0.0

	// NGA-West1 NSHMP 2008

	/** @see BooreAtkinson_2008 */
	BA_08(BooreAtkinson_2008.class, BooreAtkinson_2008.NAME, BooreAtkinson_2008.COEFFS),

	/** @see CampbellBozorgnia_2008 */
	CB_08(CampbellBozorgnia_2008.class, CampbellBozorgnia_2008.NAME, CampbellBozorgnia_2008.COEFFS),

	/** @see ChiouYoungs_2008 */
	CY_08(ChiouYoungs_2008.class, ChiouYoungs_2008.NAME, ChiouYoungs_2008.COEFFS),

	// NGA-West2 NSHMP 2014

	/** @see AbrahamsonEtAl_2014 */
	ASK_14(AbrahamsonEtAl_2014.class, AbrahamsonEtAl_2014.NAME, AbrahamsonEtAl_2014.COEFFS),

	/** @see BooreEtAl_2014 */
	BSSA_14(BooreEtAl_2014.class, BooreEtAl_2014.NAME, BooreEtAl_2014.COEFFS),

	/** @see CampbellBozorgnia_2014 */
	CB_14(CampbellBozorgnia_2014.class, CampbellBozorgnia_2014.NAME, CampbellBozorgnia_2014.COEFFS),

	/** @see ChiouYoungs_2014 */
	CY_14(ChiouYoungs_2014.class, ChiouYoungs_2014.NAME, ChiouYoungs_2014.COEFFS),

	/** @see Idriss_2014 */
	IDRISS_14(Idriss_2014.class, Idriss_2014.NAME, Idriss_2014.COEFFS),

	// Subduction NSHMP 2008 2014

	/** @see AtkinsonBoore_2003 */
	AB_03_GLOB_INTER(AtkinsonBoore_2003.GlobalInterface.class,
			AtkinsonBoore_2003.GlobalInterface.NAME, AtkinsonBoore_2003.COEFFS_GLOBAL_INTERFACE),

	/** @see AtkinsonBoore_2003 */
	AB_03_GLOB_SLAB(AtkinsonBoore_2003.GlobalSlab.class, AtkinsonBoore_2003.GlobalSlab.NAME,
			AtkinsonBoore_2003.COEFFS_GLOBAL_SLAB),

	/** @see AtkinsonBoore_2003 */
	AB_03_CASC_INTER(AtkinsonBoore_2003.CascadiaInterface.class,
			AtkinsonBoore_2003.CascadiaInterface.NAME, AtkinsonBoore_2003.COEFFS_CASC_INTERFACE),

	/** @see AtkinsonBoore_2003 */
	AB_03_CASC_SLAB(AtkinsonBoore_2003.CascadiaSlab.class, AtkinsonBoore_2003.CascadiaSlab.NAME,
			AtkinsonBoore_2003.COEFFS_CASC_SLAB),

	/** @see AtkinsonMacias_2009 */
	AM_09_INTER(AtkinsonMacias_2009.class, AtkinsonMacias_2009.NAME, AtkinsonMacias_2009.COEFFS),

	/** @see BcHydro_2012 */
	BCHYDRO_12_INTER(BcHydro_2012.Interface.class, BcHydro_2012.Interface.NAME, BcHydro_2012.COEFFS),

	/** @see BcHydro_2012 */
	BCHYDRO_12_SLAB(BcHydro_2012.Slab.class, BcHydro_2012.Slab.NAME, BcHydro_2012.COEFFS),

	/** @see YoungsEtAl_1997 */
	YOUNGS_97_INTER(YoungsEtAl_1997.Interface.class, YoungsEtAl_1997.Interface.NAME,
			YoungsEtAl_1997.COEFFS),

	/** @see YoungsEtAl_1997 */
	YOUNGS_97_SLAB(YoungsEtAl_1997.Slab.class, YoungsEtAl_1997.Slab.NAME, YoungsEtAl_1997.COEFFS),

	/** @see ZhaoEtAl_2006 */
	ZHAO_06_INTER(ZhaoEtAl_2006.Interface.class, ZhaoEtAl_2006.Interface.NAME, ZhaoEtAl_2006.COEFFS),

	/** @see ZhaoEtAl_2006 */
	ZHAO_06_SLAB(ZhaoEtAl_2006.Slab.class, ZhaoEtAl_2006.Slab.NAME, ZhaoEtAl_2006.COEFFS),

	/*
	 * Base implementations of the Gmm used in the 2008 CEUS model all work with
	 * and assume magnitude = Mw. The method converter() is provided to allow
	 * subclasses to impose a conversion if necessary.
	 * 
	 * All CEUS models impose a clamp on median ground motions; see
	 * GmmUtils.ceusMeanClip()
	 */

	// Stable continent (CEUS) NSHMP 2008 2014

	/** @see AtkinsonBoore_2006p */
	AB_06_PRIME(AtkinsonBoore_2006p.class, AtkinsonBoore_2006p.NAME, AtkinsonBoore_2006p.COEFFS),

	/** @see AtkinsonBoore_2006 */
	AB_06_140BAR(AtkinsonBoore_2006.StressDrop_140bar.class,
			AtkinsonBoore_2006.StressDrop_140bar.NAME,
			AtkinsonBoore_2006.COEFFS_A),

	/** @see AtkinsonBoore_2006 */
	AB_06_200BAR(AtkinsonBoore_2006.StressDrop_200bar.class,
			AtkinsonBoore_2006.StressDrop_200bar.NAME,
			AtkinsonBoore_2006.COEFFS_A),

	/** @see Atkinson_2008p */
	ATKINSON_08_PRIME(Atkinson_2008p.class, Atkinson_2008p.NAME, Atkinson_2008p.COEFFS),

	/** @see Campbell_2003 */
	CAMPBELL_03(Campbell_2003.class, Campbell_2003.NAME, Campbell_2003.COEFFS),

	/** @see FrankelEtAl_1996 */
	FRANKEL_96(FrankelEtAl_1996.class, FrankelEtAl_1996.NAME, FrankelEtAl_1996.COEFFS),

	/** @see PezeshkEtAl_2011 */
	PEZESHK_11(PezeshkEtAl_2011.class, PezeshkEtAl_2011.NAME, PezeshkEtAl_2011.COEFFS),

	/** @see SilvaEtAl_2002 */
	SILVA_02(SilvaEtAl_2002.class, SilvaEtAl_2002.NAME, SilvaEtAl_2002.COEFFS),

	/** @see SomervilleEtAl_2001 */
	SOMERVILLE_01(SomervilleEtAl_2001.class, SomervilleEtAl_2001.NAME, SomervilleEtAl_2001.COEFFS),

	/** @see TavakoliPezeshk_2005 */
	TP_05(TavakoliPezeshk_2005.class, TavakoliPezeshk_2005.NAME, TavakoliPezeshk_2005.COEFFS),

	/** @see ToroEtAl_1997 */
	TORO_97_MW(ToroEtAl_1997.Mw.class, ToroEtAl_1997.Mw.NAME, ToroEtAl_1997.COEFFS_MW),

	// Johnston mag converting flavors of CEUS, NSHMP 2008

	/** @see AtkinsonBoore_2006 */
	AB_06_140BAR_J(AtkinsonBoore_2006_140bar_J.class, AtkinsonBoore_2006_140bar_J.NAME,
			AtkinsonBoore_2006.COEFFS_A),

	/** @see AtkinsonBoore_2006 */
	AB_06_200BAR_J(AtkinsonBoore_2006_200bar_J.class, AtkinsonBoore_2006_200bar_J.NAME,
			AtkinsonBoore_2006.COEFFS_A),

	/** @see Campbell_2003 */
	CAMPBELL_03_J(Campbell_2003_J.class, Campbell_2003_J.NAME, Campbell_2003.COEFFS),

	/** @see FrankelEtAl_1996 */
	FRANKEL_96_J(FrankelEtAl_1996_J.class, FrankelEtAl_1996_J.NAME, FrankelEtAl_1996.COEFFS),

	/** @see SilvaEtAl_2002 */
	SILVA_02_J(SilvaEtAl_2002_J.class, SilvaEtAl_2002_J.NAME, SilvaEtAl_2002.COEFFS),

	/** @see TavakoliPezeshk_2005 */
	TP_05_J(TavakoliPezeshk_2005_J.class, TavakoliPezeshk_2005_J.NAME, TavakoliPezeshk_2005.COEFFS),

	// Atkinson Boore mag converting flavors of CEUS, NSHMP 2008

	/** @see AtkinsonBoore_2006 */
	AB_06_140BAR_AB(AtkinsonBoore_2006_140bar_AB.class, AtkinsonBoore_2006_140bar_AB.NAME,
			AtkinsonBoore_2006.COEFFS_A),

	/** @see AtkinsonBoore_2006 */
	AB_06_200BAR_AB(AtkinsonBoore_2006_200bar_AB.class, AtkinsonBoore_2006_200bar_AB.NAME,
			AtkinsonBoore_2006.COEFFS_A),

	/** @see Campbell_2003 */
	CAMPBELL_03_AB(Campbell_2003_AB.class, Campbell_2003_AB.NAME, Campbell_2003.COEFFS),

	/** @see FrankelEtAl_1996 */
	FRANKEL_96_AB(FrankelEtAl_1996_AB.class, FrankelEtAl_1996_AB.NAME, FrankelEtAl_1996.COEFFS),

	/** @see SilvaEtAl_2002 */
	SILVA_02_AB(SilvaEtAl_2002_AB.class, SilvaEtAl_2002_AB.NAME, SilvaEtAl_2002.COEFFS),

	/** @see TavakoliPezeshk_2005 */
	TP_05_AB(TavakoliPezeshk_2005_AB.class, TavakoliPezeshk_2005_AB.NAME,
			TavakoliPezeshk_2005.COEFFS),

	// - not specified
	/** @see ToroEtAl_1997 */
	TORO_97_MB(ToroEtAl_1997.Mb.class, ToroEtAl_1997.Mb.NAME, ToroEtAl_1997.COEFFS_MW),

	// Other

	/** @see SadighEtAl_1997 */
	SADIGH_97(SadighEtAl_1997.class, SadighEtAl_1997.NAME, SadighEtAl_1997.COEFFS_BC_HI),

	/** @see McVerryEtAl_2000 */
	MCVERRY_00_CRUSTAL(McVerryEtAl_2000.Crustal.class, McVerryEtAl_2000.Crustal.NAME,
			McVerryEtAl_2000.COEFFS_GM),

	/** @see McVerryEtAl_2000 */
	MCVERRY_00_INTERFACE(McVerryEtAl_2000.Interface.class, McVerryEtAl_2000.Interface.NAME,
			McVerryEtAl_2000.COEFFS_GM),

	/** @see McVerryEtAl_2000 */
	MCVERRY_00_SLAB(McVerryEtAl_2000.Slab.class, McVerryEtAl_2000.Slab.NAME,
			McVerryEtAl_2000.COEFFS_GM),

	/** @see McVerryEtAl_2000 */
	MCVERRY_00_VOLCANIC(McVerryEtAl_2000.Volcanic.class, McVerryEtAl_2000.Volcanic.NAME,
			McVerryEtAl_2000.COEFFS_GM);

	// TODO clean?
	// GK_2013(GraizerKalkan_2013.class);

	// TODO all the methods of this class need argument checking and unit tests

	private final Class<? extends GroundMotionModel> delegate;
	private final String name;
	private final Set<Imt> imts;
	private final LoadingCache<Imt, GroundMotionModel> cache;

	private Gmm(Class<? extends GroundMotionModel> delegate, String name, CoefficientContainer coeffs) {
		this.delegate = delegate;
		this.name = name;
		imts = coeffs.imts();
		cache = CacheBuilder.newBuilder().build(new CacheLoader<Imt, GroundMotionModel>() {
			@Override public GroundMotionModel load(Imt imt) throws Exception {
				return createInstance(imt);
			}
		});
	}

	private GroundMotionModel createInstance(Imt imt) throws Exception {
		Constructor<? extends GroundMotionModel> con = delegate.getDeclaredConstructor(Imt.class);
		GroundMotionModel gmm = con.newInstance(imt);
		return gmm;
	}

	/**
	 * Retreives an instance of a {@code GroundMotionModel}, either by creating
	 * a new one, or fetching from a cache.
	 * @param imt intensity measure type of instance
	 * @return the model implementation
	 * @throws UncheckedExecutionException if there is an instantiation problem
	 */
	public GroundMotionModel instance(Imt imt) {
		return cache.getUnchecked(imt);
	}

	/**
	 * Retrieves multiple {@code GroundMotionModel} instances, either by
	 * creating new ones, or fetching them from a cache.
	 * @param gmms to retrieve
	 * @param imt
	 * @return a {@code Map} of {@code GroundMotionModel} instances
	 * @throws UncheckedExecutionException if there is an instantiation problem
	 */
	public static Map<Gmm, GroundMotionModel> instances(Set<Gmm> gmms, Imt imt) {
		Map<Gmm, GroundMotionModel> instances = Maps.newEnumMap(Gmm.class);
		for (Gmm gmm : gmms) {
			instances.put(gmm, gmm.instance(imt));
		}
		return instances;
	}

	// TODO deprecate/delete above??

	/**
	 * Retrieves a {@code Table} of {@code GroundMotionModel} instances for a
	 * range of {@code Imt}s, either by creating new ones, or fetching them from
	 * a cache.
	 * @param gmms to retrieve
	 * @param imts
	 * @return a {@code Table} of {@code GroundMotionModel} instances
	 * @throws UncheckedExecutionException if there is an instantiation problem
	 */
	public static Table<Gmm, Imt, GroundMotionModel> instances(Set<Gmm> gmms, Set<Imt> imts) {
		Table<Gmm, Imt, GroundMotionModel> instances = ArrayTable.create(gmms, imts);
		for (Gmm gmm : gmms) {
			for (Imt imt : imts) {
				instances.put(gmm, imt, gmm.instance(imt));
			}
		}
		return instances;
	}

	@Override public String toString() {
		return name;
	}

	/**
	 * Returns the {@code Set} of the intensity measure types ({@code Imt}s)
	 * supported by this {@code Gmm}.
	 * @return the {@code Set} of supported {@code Imt}s
	 */
	public Set<Imt> supportedIMTs() {
		return imts;
	}

	/**
	 * Returns the {@code Set} of the intensity measure types ({@code Imt}s)
	 * supported by all of the supplied {@code Gmm}s.
	 * @param gmms models for which to return common {@code Imt} supoort
	 * @return the {@code Set} of supported {@code Imt}s
	 */
	public static Set<Imt> supportedIMTs(Collection<Gmm> gmms) {
		Set<Imt> imts = EnumSet.allOf(Imt.class);
		for (Gmm gmm : gmms) {
			imts = Sets.intersection(imts, gmm.supportedIMTs());
		}
		return EnumSet.copyOf(imts);
	}

	/**
	 * Returns the set of spectral acceleration {@code Imt}s that are supported
	 * by this {@code Gmm}.
	 * @return a {@code Set} of spectral acceleration IMTs
	 */
	public Set<Imt> responseSpectrumIMTs() {
		return Sets.intersection(imts, Imt.saImts());
	}

	/**
	 * Returns the set of spectral acceleration {@code Imt}s that are common to
	 * the supplied {@code Gmm}s.
	 * 
	 * @param gmms ground motion models
	 * @return a {@code Set} of common spectral acceleration {@code Imt}s
	 */
	public static Set<Imt> responseSpectrumIMTs(Collection<Gmm> gmms) {
		return Sets.intersection(supportedIMTs(gmms), Imt.saImts());
	}

}