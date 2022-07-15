/*
  A benchmark for NuSMV extensions to BeepBeep 3
  Copyright (C) 2021 Alexis Bédard and Sylvain Hallé

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package nusmvlab;

import ca.uqac.lif.labpal.experiment.SingleClassExperimentFactory;
import ca.uqac.lif.labpal.region.Point;
import ca.uqac.lif.labpal.region.Region;

import static nusmvlab.ModelProvider.GENERATION_TIME;

/**
 * Creates instances of {@link NuSMVExperiment} based on parameters found in
 * a {@link Region}.
 */
public class NuSMVExperimentFactory extends SingleClassExperimentFactory<NuSMVExperiment>
{
	/**
	 * A library to provide NUSMV models.
	 */
	protected transient Library<ModelProvider> m_modelLibrary;
	
	/**
	 * A library to provide properties to verify on models.
	 */
	protected transient Library<PropertyProvider> m_propertyLibrary;
	
	/**
	 * Sets whether experiments should gather extra stats about state space
	 * size.
	 */
	protected boolean m_withStats;
	
	/**
	 * Creates a new instance of the factory
	 * @param lab The lab the experiments will be added to
	 * @param models A library that provides models based on a region
	 * @param props A library that provides properties based on a region
	 * @param with_stats Set to true to make experiments gather extra stats
	 * about state space size. Activating this option takes much longer than
	 * simply checking properties.
	 */
	public NuSMVExperimentFactory(MainLab lab, Library<ModelProvider> models, Library<PropertyProvider> props)
	{
		super(lab, NuSMVExperiment.class);
		m_modelLibrary = models;
		m_propertyLibrary = props;
		m_withStats = false;
	}
	
	/**
	 * Sets the factory so that experiments gather extra stats about state
	 * space size. Activating this option takes much longer than simply checking
	 * properties.
	 */
	public void addStats()
	{
		m_withStats = true;
	}
	
	@Override
	protected NuSMVExperiment createExperiment(Point region)
	{
		long start = System.currentTimeMillis();
		ModelProvider model = m_modelLibrary.get(region);
		long end = System.currentTimeMillis();
		PropertyProvider prop = m_propertyLibrary.get(region);
		if (model == null || prop == null)
		{
			return null;
		}
		NuSMVExperiment e = new NuSMVExperiment(model, prop, m_withStats);
		e.writeOutput(GENERATION_TIME, end - start);
		return e;
	}
}
