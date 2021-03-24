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
import ca.uqac.lif.labpal.Region;

import static nusmvlab.BeepBeepModelProvider.DOMAIN_SIZE;
import static nusmvlab.BeepBeepModelProvider.QUERY;
import static nusmvlab.BeepBeepModelProvider.QUEUE_SIZE;

/**
 * Library that produces NUSMV model providers based on the contents of a
 * region.
 */
public class NuSMVModelLibrary implements Library<ModelProvider>
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
	 * Creates a new instance of the library.
	 */
	public NuSMVModelLibrary()
	{
		super();
	}
	
	@Override
	public ModelProvider get(Region r)
	{
		String query = r.getString(QUERY);
		int domain_size = r.getInt(DOMAIN_SIZE);
		int queue_size = r.getInt(QUEUE_SIZE);
		if (query.compareTo(Q_DUMMY) == 0)
		{
			return new DummyModelProvider(queue_size, domain_size);
		}
		Processor start = getProcessorChain(query);
		if (start == null)
		{
			return null;
		}
		return null;
	}
	
	/**
	 * Creates a chain of BeepBeep processors, based on a textual name.
	 * This method is used internally by {@link #getModel(Region, int, int)}. 
	 * @param query The name of the chain to create
	 * @return A reference to the first processor of the chain
	 */
	protected static Processor getProcessorChain(String query)
	{
		if (query.compareTo(Q_PASSTHROUGH) == 0)
		{
			return new Passthrough();
		}
		return null;
	}
}
