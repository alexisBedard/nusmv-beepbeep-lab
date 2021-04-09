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

import ca.uqac.lif.cep.Connector;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.CumulativeFunction;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Passthrough;
import ca.uqac.lif.cep.tmf.Window;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.labpal.Region;

import static nusmvlab.BeepBeepModelProvider.DOMAIN_SIZE;
import static nusmvlab.BeepBeepModelProvider.QUERY;
import static nusmvlab.BeepBeepModelProvider.QUEUE_SIZE;
import static nusmvlab.BeepBeepModelProvider.K;

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
	 * The name of query "Sum of window of width 3"
	 */
	public static final transient String Q_SUM_3 = "Sum of window of width 3";
	
	/**
	 * The name of query "Sum of 1s"
	 */
	public static final transient String Q_WIN_SUM_OF_1 = "Sum of 1s on window";
	
	/**
	 * The name of query "1 times k"
	 */
	public static final transient String Q_SUM_OF_f1 = "Sum of 1s on window ";
	
	/**
	 * The name of query "Sum of doubles"
	 */
	public static final transient String Q_SUM_OF_DOUBLES = "Sum of doubles";
	
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
		int k = -1;
		if (r.hasDimension(K))
		{
			k = r.getInt(K);
		}
		if (query.compareTo(Q_DUMMY) == 0)
		{
			return new DummyModelProvider(queue_size, domain_size);
		}
		Processor start = getProcessorChain(query, k);
		if (start == null)
		{
			return null;
		}
		return new BeepBeepModelProvider(start, query, queue_size, domain_size, k);
	}
	
	/**
	 * Creates a chain of BeepBeep processors, based on a textual name.
	 * This method is used internally by {@link #getModel(Region, int, int)}. 
	 * @param query The name of the chain to create
	 * @return A reference to the first processor of the chain
	 */
	protected static Processor getProcessorChain(String query, int k)
	{
		if (query.compareTo(Q_PASSTHROUGH) == 0)
		{
			return new Passthrough();
		}
		if (query.compareTo(Q_SUM_3) == 0)
		{
			// Window width is 3 if not specified
			int width = k > 0 ? k : 3;
			return new Window(new Cumulate(new CumulativeFunction<Number>(Numbers.addition)), width);
		}
		if (query.compareTo(Q_SUM_OF_DOUBLES) == 0)
		{
			Fork f = new Fork();
			ApplyFunction mul = new ApplyFunction(Numbers.multiplication);
			TurnInto two = new TurnInto(2);
			Connector.connect(f, 0, mul, 0);
			Connector.connect(f, 0, two, 0);
			Connector.connect(two, 0, mul, 1);
			Cumulate sum = new Cumulate(new CumulativeFunction<Number>(Numbers.addition));
			Connector.connect(mul, sum);
			return f;
		}
		return null;
	}
}
