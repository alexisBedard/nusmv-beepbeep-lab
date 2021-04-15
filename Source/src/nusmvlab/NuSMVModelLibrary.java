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
import ca.uqac.lif.cep.GroupProcessor;
import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.functions.ApplyFunction;
import ca.uqac.lif.cep.functions.Cumulate;
import ca.uqac.lif.cep.functions.CumulativeFunction;
import ca.uqac.lif.cep.functions.TurnInto;
import ca.uqac.lif.cep.tmf.CountDecimate;
import ca.uqac.lif.cep.tmf.Filter;
import ca.uqac.lif.cep.tmf.Fork;
import ca.uqac.lif.cep.tmf.Passthrough;
import ca.uqac.lif.cep.tmf.Trim;
import ca.uqac.lif.cep.tmf.Window;
import ca.uqac.lif.cep.util.Equals;
import ca.uqac.lif.cep.util.Numbers;
import ca.uqac.lif.labpal.Region;
import nusmvlab.StreamPropertyLibrary.OutputAlwaysTrue;

import static nusmvlab.BeepBeepModelProvider.DOMAIN_SIZE;
import static nusmvlab.PropertyProvider.PROPERTY;
import static nusmvlab.BeepBeepModelProvider.QUERY;
import static nusmvlab.BeepBeepModelProvider.QUEUE_SIZE;

import java.util.HashMap;
import java.util.Map;

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
	public static final transient String Q_PRODUCT_WINDOW_K = "Sum of window of width 3";

	/**
	 * The name of query "Sum of 1s"
	 */
	public static final transient String Q_WIN_SUM_OF_1 = "Sum of 1s on window";

	/**
	 * The name of query "Sum of doubles"
	 */
	public static final transient String Q_SUM_OF_DOUBLES = "Sum of doubles";

	/**
	 * The name of query "Product"
	 */
	public static final transient String Q_PRODUCT = "Product";

	/**
	 * The name of query "Product of 1 and k-th"
	 */
	public static final transient String Q_PRODUCT_1_K = "Product of 1 and k-th";

	/**
	 * The name of query "Output if smaller than k"
	 */
	public static final transient String Q_OUTPUT_IF_SMALLER_K = "Output if smaller than k";

	/**
	 * The name of query "Window sum of 2 comparison"
	 */
	public static final transient String Q_COMPARE_WINDOW_SUM_2 = "Window sum of 2 comparison";

	/**
	 * The name of query "Window sum of 3 comparison"
	 */
	public static final transient String Q_COMPARE_WINDOW_SUM_3 = "Window sum of 3 comparison";

	/**
	 * The name of query "Window sum of k comparison"
	 */
	public static final transient String Q_COMPARE_PASSTHROUGH_DELAY = "Passthrough vs delay comparison";

	/**
	 * A cache of pipelines already generated. When requested another time,
	 * the chain is fetched from this map instead of being regenerated. The issue
	 * here is not performance, but rather that every time the chain is created,
	 * its processors are given different unique IDs; this could create a
	 * mismatch between NuSMV variables for the chain, and those that occur in
	 * the CTL/LTL formulas (which are generated in a later step).
	 */
	protected transient Map<ModelId,Processor> m_cache;

	/**
	 * Creates a new instance of the library.
	 */
	public NuSMVModelLibrary()
	{
		super();
		m_cache = new HashMap<ModelId,Processor>();
	}

	/**
	 * Gets the names of all queries handled by this model provider.
	 * @return The names of all queries
	 */
	public static String[] getQueryNames()
	{
		return new String[] {Q_PASSTHROUGH, Q_PRODUCT_WINDOW_K, Q_WIN_SUM_OF_1,
				Q_SUM_OF_DOUBLES, Q_PRODUCT, Q_PRODUCT_1_K,
				Q_OUTPUT_IF_SMALLER_K, Q_COMPARE_WINDOW_SUM_3};
	}

	@Override
	public ModelProvider get(Region r)
	{
		String query = r.getString(QUERY);
		int domain_size = r.getInt(DOMAIN_SIZE);
		int queue_size = r.getInt(QUEUE_SIZE);
		Count c = new Count();
		c.x = -1;
		if (r.hasDimension(K))
		{
			c.x = r.getInt(K);
		}
		if (query.compareTo(Q_DUMMY) == 0)
		{
			return new DummyModelProvider(queue_size, domain_size);
		}
		ModelId m = new ModelId(r);
		Processor start = null;
		if (m_cache.containsKey(m)) 
		{
			start = m_cache.get(m);
		}
		else
		{
			start = getProcessorChain(r, c);
			m_cache.put(m, start);
		}
		if (start == null)
		{
			return null;
		}
		return new BeepBeepModelProvider(start, query, queue_size, domain_size, c.x, getImageUrl(query));
	}

	/**
	 * Creates a chain of BeepBeep processors, based on a textual name.
	 * This method is used internally by {@link #getModel(Region, int, int)}. 
	 * @param r The region corresponding to the chain to create
	 * @param query The name of the chain to create
	 * @return A reference to the first processor of the chain
	 */
	protected static Processor getProcessorChain(Region r, Count c)
	{
		String query = r.getString(QUERY);
		String property = r.getString(PROPERTY);
		if (query.compareTo(Q_PASSTHROUGH) == 0)
		{
			return new Passthrough();
		}
		if (query.compareTo(Q_PRODUCT) == 0)
		{
			return new Cumulate(new CumulativeFunction<Number>(Numbers.multiplication));
		}
		if (query.compareTo(Q_PRODUCT_1_K) == 0)
		{
			// Decimation interval is 3 if not specified
			c.x = c.x > 0 ? c.x : 3;
			Fork f = new Fork();
			ApplyFunction mul = new ApplyFunction(Numbers.multiplication);
			CountDecimate dec = new CountDecimate(c.x);
			Connector.connect(f, 0, mul, 0);
			Connector.connect(f, 0, dec, 0);
			Connector.connect(dec, 0, mul, 1);
			return f;
		}
		if (query.compareTo(Q_PRODUCT_WINDOW_K) == 0)
		{
			// Window width is 3 if not specified
			c.x = c.x > 0 ? c.x : 3;
			return new Window(new Cumulate(new CumulativeFunction<Number>(Numbers.multiplication)), c.x);
		}
		if (query.compareTo(Q_SUM_OF_DOUBLES) == 0)
		{
			if (r.getInt(DOMAIN_SIZE) < 3)
			{
				// This query is only possible if domain contains number 2
				return null;
			}
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
		if (query.compareTo(Q_WIN_SUM_OF_1) == 0)
		{
			// Window width is 3 if not specified
			c.x = c.x > 0 ? c.x : 3;
			TurnInto one = new TurnInto(1);
			Cumulate sum = new Cumulate(new CumulativeFunction<Number>(Numbers.addition));
			Connector.connect(one, sum);
			Window win = new Window(new Cumulate(new CumulativeFunction<Number>(Numbers.multiplication)), c.x);
			Connector.connect(sum, win);
			return one;
		}
		if (query.compareTo(Q_OUTPUT_IF_SMALLER_K) == 0)
		{
			// Parameter value is 3 if not specified
			c.x = c.x > 0 ? c.x : 3;
			if (r.getInt(DOMAIN_SIZE) <= c.x)
			{
				// This query is only possible if domain contains number k
				return null;
			}
			Fork f = new Fork(3);
			Filter filter = new Filter();
			Connector.connect(f, 0, filter, 0);
			TurnInto turn_k = new TurnInto(c.x);
			Connector.connect(f, 1, turn_k, 0);
			TurnInto turn_1 = new TurnInto(1);
			Connector.connect(f, 2, turn_1, 0);
			Cumulate sum = new Cumulate(new CumulativeFunction<Number>(Numbers.addition));
			Connector.connect(turn_1, sum);
			ApplyFunction gt = new ApplyFunction(Numbers.isGreaterOrEqual);
			Connector.connect(turn_k, 0, gt, 0);
			Connector.connect(sum, 0, gt, 1);
			Connector.connect(gt, 0, filter, 1);
			return f;
		}
		if (query.compareTo(Q_COMPARE_WINDOW_SUM_3) == 0)
		{
			GroupProcessor g1 = new GroupProcessor(1, 1);
			{
				Cumulate sum = new Cumulate(new CumulativeFunction<Number>(Numbers.addition));
				Window win = new Window(sum, 3);
				g1.addProcessor(win);
				g1.associateInput(0, win, 0);
				g1.associateOutput(0, win, 0);
			}
			GroupProcessor g2 = new GroupProcessor(1, 1);
			{
				Fork f = new Fork(3);
				Trim trim1 = new Trim(1);
				ApplyFunction add1 = new ApplyFunction(Numbers.addition);
				Connector.connect(f, 0, add1, 0);
				Connector.connect(f, 1, trim1, 0);
				Connector.connect(trim1, 0, add1, 1);
				Trim trim2 = new Trim(2);
				ApplyFunction add2 = new ApplyFunction(Numbers.addition);
				Connector.connect(f, 2, trim2, 0);
				Connector.connect(add1, 0, add2, 0);
				Connector.connect(trim2, 0, add2, 1);
				g2.addProcessors(f, trim1, add1, trim2, add2);
				g2.associateInput(0, f, 0);
				g2.associateOutput(0, add2, 0);
			}
			Fork f = new Fork();
			Connector.connect(f, 0, g1, 0);
			Connector.connect(f, 1, g2, 0);
			if (property.compareTo(OutputAlwaysTrue.NAME) == 0)
			{
				ApplyFunction equals = new ApplyFunction(Equals.instance);
				Connector.connect(g1, 0, equals, 0);
				Connector.connect(g2, 0, equals, 1);				
			}
			return f;
		}
		if (query.compareTo(Q_COMPARE_WINDOW_SUM_2) == 0)
		{
			GroupProcessor g1 = new GroupProcessor(1, 1);
			{
				Cumulate sum = new Cumulate(new CumulativeFunction<Number>(Numbers.addition));
				Window win = new Window(sum, 2);
				g1.addProcessor(win);
				g1.associateInput(0, win, 0);
				g1.associateOutput(0, win, 0);
			}
			GroupProcessor g2 = new GroupProcessor(1, 1);
			{
				Fork f = new Fork();
				Trim trim1 = new Trim(1);
				ApplyFunction add1 = new ApplyFunction(Numbers.addition);
				Connector.connect(f, 0, add1, 0);
				Connector.connect(f, 1, trim1, 0);
				Connector.connect(trim1, 0, add1, 1);
				g2.addProcessors(f, trim1, add1);
				g2.associateInput(0, f, 0);
				g2.associateOutput(0, add1, 0);
			}
			Fork f = new Fork();
			Connector.connect(f, 0, g1, 0);
			Connector.connect(f, 1, g2, 0);
			if (property.compareTo(OutputAlwaysTrue.NAME) == 0)
			{
				ApplyFunction equals = new ApplyFunction(Equals.instance);
				Connector.connect(g1, 0, equals, 0);
				Connector.connect(g2, 0, equals, 1);				
			}
			return f;
		}
		return null;
	}

	/**
	 * Gets the image URL associated to a processor chain.
	 * @param query The name of the processor chain
	 * @return The URL, or <tt>null</tt> if no image is available
	 */
	protected static String getImageUrl(String query)
	{
		if (query.compareTo(Q_PASSTHROUGH) == 0)
		{
			return "/resource/Passthrough.png";
		}
		if (query.compareTo(Q_PRODUCT) == 0)
		{
			return "/resource/Product.png";
		}
		if (query.compareTo(Q_PRODUCT_1_K) == 0)
		{
			return "/resource/Product1_k.png";
		}
		if (query.compareTo(Q_SUM_OF_DOUBLES) == 0)
		{
			return "/resource/SumOfDoubles.png";
		}
		if (query.compareTo(Q_PRODUCT_WINDOW_K) == 0)
		{
			return "/resource/ProductWindow_k.png";
		}
		if (query.compareTo(Q_WIN_SUM_OF_1) == 0)
		{
			return "/resource/SumOfOnesWindow_k.png";
		}
		if (query.compareTo(Q_OUTPUT_IF_SMALLER_K) == 0)
		{
			return "/resource/OutputIfSmallerThan_k.png";
		}
		if (query.compareTo(Q_COMPARE_WINDOW_SUM_2) == 0)
		{
			return "/resource/CompareWindowSum2.png";
		}
		if (query.compareTo(Q_COMPARE_WINDOW_SUM_3) == 0)
		{
			return "/resource/CompareWindowSum3.png";
		}
		return null;
	}

	/**
	 * Ugly hack to pass an integer by reference to a method.
	 */
	protected static class Count
	{
		public int x = 0;
	}
}
