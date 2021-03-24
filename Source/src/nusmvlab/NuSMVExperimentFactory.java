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

import ca.uqac.lif.labpal.ExperimentFactory;
import ca.uqac.lif.labpal.Region;

/**
 * Creates instances of {@link NuSMVExperiment} based on parameters found in
 * a {@link Region}.
 */
public class NuSMVExperimentFactory extends ExperimentFactory<MainLab,NuSMVExperiment>
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
	 * Creates a new instance of the factory
	 * @param lab The lab the experiments will be added to
	 * @param models A library that provides models based on a region
	 * @param props A library that provides properties based on a region
	 */
	public NuSMVExperimentFactory(MainLab lab, Library<ModelProvider> models, Library<PropertyProvider> props)
	{
		super(lab, NuSMVExperiment.class);
		m_modelLibrary = models;
		m_propertyLibrary = props;
	}

	@Override
	protected NuSMVExperiment createExperiment(Region region)
	{
		ModelProvider model = m_modelLibrary.get(region);
		PropertyProvider prop = m_propertyLibrary.get(region);
		if (model == null || prop == null)
		{
			return null;
		}
		NuSMVExperiment e = new NuSMVExperiment(model, prop);
		return e;
	}
}
