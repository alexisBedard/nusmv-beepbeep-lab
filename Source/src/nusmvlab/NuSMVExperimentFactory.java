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

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.tmf.Passthrough;
import ca.uqac.lif.labpal.ExperimentFactory;
import ca.uqac.lif.labpal.Region;

import static nusmvlab.BeepBeepModelProvider.DOMAIN_SIZE;
import static nusmvlab.BeepBeepModelProvider.QUERY;
import static nusmvlab.BeepBeepModelProvider.QUEUE_SIZE;

/**
 * Creates instances of {@link NuSMVExperiment} based on parameters found in
 * a {@link Region}.
 */
public class NuSMVExperimentFactory extends ExperimentFactory<MainLab,NuSMVExperiment>
{
	/**
	 * The name of query "Dummy"
	 */
	public static final transient String Q_DUMMY = "Dummy";
	
	/**
	 * The name of query "Passthrough"
	 */
	public static final transient String Q_PASSTHROUGH = "Passthrough";
	
	/**
	 * Creates a new instance of the factory
	 * @param lab The lab the experiments will be added to
	 */
	public NuSMVExperimentFactory(MainLab lab)
	{
		super(lab, NuSMVExperiment.class);
	}

	@Override
	protected NuSMVExperiment createExperiment(Region region)
	{
		String query = region.getString(QUERY);
		if (query == null)
		{
			return null;
		}
		Processor start = createProcessorChain(query);
		if (start == null)
		{
			return null;
		}
		int queue_size = region.getInt(QUEUE_SIZE);
		int domain_size = region.getInt(DOMAIN_SIZE);
		NuSMVModelProvider bmp = null;
		if (query.compareTo(Q_DUMMY) == 0)
		{
			// Special case for testing purposes
			bmp = new DummyModelProvider(queue_size, domain_size);
		}
		else
		{
			bmp = new BeepBeepModelProvider(start, query, queue_size, domain_size);
		}
		NuSMVExperiment e = new NuSMVExperiment(bmp);
		return e;
	}
	
	/**
	 * Creates a chain of BeepBeep processors, based on a textual name.
	 * @param query The name of the chain to create
	 * @return A reference to the first processor of the chain
	 */
	protected static Processor createProcessorChain(String query)
	{
		if (query.compareTo(Q_PASSTHROUGH) == 0)
		{
			return new Passthrough();
		}
		return null;
	}
}
