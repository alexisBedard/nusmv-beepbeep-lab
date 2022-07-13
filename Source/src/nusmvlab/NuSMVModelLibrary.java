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

import ca.uqac.lif.cep.nusmv.BeepBeepModel;
import ca.uqac.lif.cep.nusmv.BeepBeepPipeline;
import ca.uqac.lif.cep.nusmv.BinaryApplyFunctionModule;
import ca.uqac.lif.cep.nusmv.CountDecimateModule;
import ca.uqac.lif.cep.nusmv.CumulateModule;
import ca.uqac.lif.cep.nusmv.EqualsFunction;
import ca.uqac.lif.cep.nusmv.FilterModule;
import ca.uqac.lif.cep.nusmv.ForkModule;
import ca.uqac.lif.cep.nusmv.GroupModule;
import ca.uqac.lif.cep.nusmv.NusmvNumbers;
import ca.uqac.lif.cep.nusmv.PassthroughModule;
import ca.uqac.lif.cep.nusmv.ProcessorModule;
import ca.uqac.lif.cep.nusmv.ProcessorQueue;
import ca.uqac.lif.cep.nusmv.TrimModule;
import ca.uqac.lif.cep.nusmv.TurnIntoModule;
import ca.uqac.lif.cep.nusmv.UnaryApplyFunctionModule;
import ca.uqac.lif.cep.nusmv.WindowModule;
import ca.uqac.lif.labpal.Region;
import ca.uqac.lif.nusmv4j.BooleanDomain;
import ca.uqac.lif.nusmv4j.Domain;
import ca.uqac.lif.nusmv4j.IntegerRange;
import nusmvlab.StreamPropertyLibrary.OutputAlwaysTrue;
import nusmvlab.StreamPropertyLibrary.OutputsAlwaysEqual;

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
	 * The name of query "product of window of width 3"
	 */
	public static final transient String Q_PRODUCT_WINDOW_K = "Product of window of width 3";

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
	 * The name of query "Sum of odds"
	 */
	public static final transient String Q_SUM_OF_ODDS = "Sum of odds";

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
	protected transient Map<ModelId,BeepBeepPipeline> m_cache;

	/**
	 * Creates a new instance of the library.
	 */
	public NuSMVModelLibrary()
	{
		super();
		m_cache = new HashMap<ModelId,BeepBeepPipeline>();
	}

	/**
	 * Gets the names of all queries handled by this model provider.
	 * @return The names of all queries
	 */
	public static String[] getQueryNames()
	{
		return new String[] {Q_PASSTHROUGH, Q_PRODUCT_WINDOW_K, Q_WIN_SUM_OF_1,
				Q_SUM_OF_DOUBLES, Q_PRODUCT, Q_PRODUCT_1_K, Q_SUM_OF_ODDS,
				Q_OUTPUT_IF_SMALLER_K, Q_COMPARE_WINDOW_SUM_2, Q_COMPARE_WINDOW_SUM_3,
				Q_COMPARE_PASSTHROUGH_DELAY};
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
		BeepBeepPipeline start = null;
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
		try
		{
			BeepBeepModelProvider bbmp = new BeepBeepModelProvider(new BeepBeepModel(start), query, queue_size, domain_size, c.x, getImageUrl(query));			
			return bbmp;
		}
		catch (RuntimeException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates a chain of BeepBeep processors, based on a textual name.
	 * This method is used internally by {@link #getModel(Region, int, int)}. 
	 * @param r The region corresponding to the chain to create
	 * @param query The name of the chain to create
	 * @return A reference to the first processor of the chain
	 */
	protected static BeepBeepPipeline getProcessorChain(Region r, Count c)
	{
		String query = r.getString(QUERY);
		String property = r.getString(PROPERTY);
		int dom_size = r.getInt(DOMAIN_SIZE);
		Domain domain = new IntegerRange(0, dom_size);
		int q_size = r.getInt(QUEUE_SIZE);
		boolean is_comparison = property.compareTo(OutputsAlwaysEqual.NAME) == 0 || property.compareTo(OutputAlwaysTrue.NAME) == 0;
		boolean is_stepwise = property.compareTo(OutputsAlwaysEqual.NAME) == 0;
		int Q_in = 1, Q_out = 1;
		if (query.compareTo(Q_PASSTHROUGH) == 0)
		{
			if (!is_comparison)
			{
				BeepBeepPipeline bp = new BeepBeepPipeline("Passthrough", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, new ProcessorQueue[] {new ProcessorQueue("o", "oc_0", "ob_0", 1, domain)});
				PassthroughModule pt = new PassthroughModule("pt", domain, Q_in);
				bp.add(pt);
				bp.setInput(pt, 0, 0);
				bp.setOutput(pt, 0, 0);
				return bp;
			}
			else
			{
				ProcessorQueue[] out_queues;
				if (is_stepwise)
				{
					out_queues = new ProcessorQueue[] {new ProcessorQueue("ou0", "oc_0", "ob_0", 1, domain), new ProcessorQueue("ou1", "oc_1", "ob_1", 1, domain)};
				}
				else
				{
					out_queues = new ProcessorQueue[] {new ProcessorQueue("ou0", "oc_0", "ob_0", 1, BooleanDomain.instance)};
				}
				BeepBeepPipeline bp = new BeepBeepPipeline("ProductOneK", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, out_queues);
				ForkModule compare_fork = new ForkModule("Fork2", domain, 2, Q_in);
				bp.add(compare_fork);
				bp.setInput(compare_fork, 0, 0);
				ProcessorModule out_1 = null, out_2 = null;
				{
					// Copy 1
					PassthroughModule pt = new PassthroughModule("pt", domain, Q_in);
					bp.add(pt);
					bp.connect(compare_fork, 0, pt, 0);
					out_1 = pt;
				}
				{
					// Copy 2
					PassthroughModule pt = new PassthroughModule("pt", domain, Q_in);
					bp.add(pt);
					bp.connect(compare_fork, 1, pt, 0);
					out_2 = pt;
				}
				if (!is_stepwise)
				{
					BinaryApplyFunctionModule comp_eq = new BinaryApplyFunctionModule("Eq", new NusmvNumbers.IsEqual(domain), Q_in, q_size, Q_out);
					bp.add(comp_eq);
					bp.connect(out_1, 0, comp_eq, 0);
					bp.connect(out_2, 0, comp_eq, 1);
					bp.setOutput(comp_eq, 0, 0);
				}
				else
				{
					bp.setOutput(out_1, 0, 0);
					bp.setOutput(out_2, 0, 1);
				}
				return bp;
			}
		}
		if (query.compareTo(Q_PRODUCT) == 0)
		{
			BeepBeepPipeline bp = new BeepBeepPipeline("Product", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, new ProcessorQueue[] {new ProcessorQueue("ou", "oc_0", "ob_0", 1, domain)});
			CumulateModule prod = new CumulateModule("prod", new NusmvNumbers.Multiplication(domain), Q_in, Q_out);
			bp.add(prod);
			bp.setInput(prod, 0, 0);
			bp.setOutput(prod, 0, 0);
			return bp;
		}
		if (query.compareTo(Q_PRODUCT_1_K) == 0)
		{
			// Decimation interval is 3 if not specified
			c.x = c.x > 0 ? c.x : 3;
			if (!is_comparison)
			{
				BeepBeepPipeline bp = new BeepBeepPipeline("ProductOneK", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, new ProcessorQueue[] {new ProcessorQueue("ou", "oc_0", "ob_0", 1, domain)});
				ForkModule f = new ForkModule("Fork2", domain, 2, Q_in);
				BinaryApplyFunctionModule mul = new BinaryApplyFunctionModule("Mul", new NusmvNumbers.Multiplication(domain), Q_in, q_size, Q_out);
				CountDecimateModule dec = new CountDecimateModule("Decimate" + c.x, c.x, domain, Q_in, Q_out);
				bp.connect(f, 0, mul, 0);
				bp.connect(f, 1, dec, 0);
				bp.connect(dec, 0, mul, 1);
				bp.add(f, mul, dec);
				bp.setInput(f, 0, 0);
				bp.setOutput(mul, 0, 0);
				return bp;
			}
			else
			{
				ProcessorQueue[] out_queues;
				if (is_stepwise)
				{
					out_queues = new ProcessorQueue[] {new ProcessorQueue("ou0", "oc_0", "ob_0", 1, domain), new ProcessorQueue("ou1", "oc_1", "ob_1", 1, domain)};
				}
				else
				{
					out_queues = new ProcessorQueue[] {new ProcessorQueue("ou0", "oc_0", "ob_0", 1, BooleanDomain.instance)};
				}
				BeepBeepPipeline bp = new BeepBeepPipeline("ProductOneK", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, out_queues);
				ForkModule compare_fork = new ForkModule("Fork2", domain, 2, Q_in);
				bp.add(compare_fork);
				bp.setInput(compare_fork, 0, 0);
				ProcessorModule out_1 = null, out_2 = null;
				{
					// Copy 1
					ForkModule f = new ForkModule("Fork2", domain, 2, Q_in);
					BinaryApplyFunctionModule mul = new BinaryApplyFunctionModule("Mul", new NusmvNumbers.Multiplication(domain), Q_in, q_size, Q_out);
					CountDecimateModule dec = new CountDecimateModule("Decimate" + c.x, c.x, domain, Q_in, Q_out);
					bp.connect(f, 0, mul, 0);
					bp.connect(f, 1, dec, 0);
					bp.connect(dec, 0, mul, 1);
					bp.add(f, mul, dec);
					bp.connect(compare_fork, 0, f, 0);
					out_1 = mul;
				}
				{
					// Copy 2
					ForkModule f = new ForkModule("Fork2", domain, 2, Q_in);
					BinaryApplyFunctionModule mul = new BinaryApplyFunctionModule("Mul", new NusmvNumbers.Multiplication(domain), Q_in, q_size, Q_out);
					CountDecimateModule dec = new CountDecimateModule("Decimate" + c.x, c.x, domain, Q_in, Q_out);
					bp.connect(f, 0, mul, 0);
					bp.connect(f, 1, dec, 0);
					bp.connect(dec, 0, mul, 1);
					bp.add(f, mul, dec);
					bp.connect(compare_fork, 1, f, 0);
					out_2 = mul;
				}
				if (!is_stepwise)
				{
					BinaryApplyFunctionModule comp_eq = new BinaryApplyFunctionModule("Eq", new NusmvNumbers.IsEqual(domain), Q_in, q_size, Q_out);
					bp.add(comp_eq);
					bp.connect(out_1, 0, comp_eq, 0);
					bp.connect(out_2, 0, comp_eq, 1);
					bp.setOutput(comp_eq, 0, 0);
				}
				else
				{
					bp.setOutput(out_1, 0, 0);
					bp.setOutput(out_2, 0, 1);
				}
				return bp;
			}
		}
		if (query.compareTo(Q_PRODUCT_WINDOW_K) == 0)
		{
			// Window width is 3 if not specified
			c.x = c.x > 0 ? c.x : 3;
			if (!is_comparison)
			{
				BeepBeepPipeline bp = new BeepBeepPipeline("ProductWindowK", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, new ProcessorQueue[] {new ProcessorQueue("ou", "oc_0", "ob_0", 1, domain)});
				CumulateModule prod = new CumulateModule("Product", new NusmvNumbers.Multiplication(domain), c.x, c.x);
				WindowModule win = new WindowModule("Win", prod, c.x, domain, domain, Q_in, Q_out);
				bp.add(win);
				bp.setInput(win, 0, 0);
				bp.setOutput(win, 0, 0);
				return bp;
			}
			else
			{
				return null;
			}
			/*
			BeepBeepPipeline bp = new BeepBeepPipeline("ProductWindowK", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, new ProcessorQueue[] {new ProcessorQueue("ou", "oc_0", "ob_0", 1, domain)});
			CumulateModule prod = new CumulateModule("Product", new NusmvNumbers.Multiplication(domain), c.x, c.x);
			WindowModule win = new WindowModule("Win", prod, c.x, domain, domain, Q_in, q_size, Q_out);
			int out_arity = 1;
			joinGroup(bp, win, win, domain, out_arity, Q_in, q_size, Q_out, property);
			return bp;
			 */
		}
		if (query.compareTo(Q_SUM_OF_ODDS) == 0)
		{
			if (!is_comparison)
			{
				BeepBeepPipeline bp = new BeepBeepPipeline("SumOfOdds", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, new ProcessorQueue[] {new ProcessorQueue("ou", "oc_0", "ob_0", 1, domain)});
				TurnIntoModule one_1 = new TurnIntoModule("TurnOne", domain, domain, 1, Q_in, Q_out);
				CumulateModule sum_1 = new CumulateModule("Sum", new NusmvNumbers.Addition(domain), Q_in, Q_out);
				bp.connect(one_1, 0, sum_1, 0);
				ForkModule f = new ForkModule("Fork3", domain, 3, 1);
				bp.connect(sum_1, 0, f, 0);
				TrimModule trim = new TrimModule("Trim1", 1, domain, 1);
				bp.connect(f, 0, trim, 0);
				UnaryApplyFunctionModule even = new UnaryApplyFunctionModule("IsEven", new NusmvNumbers.IsEven(domain), Q_in, Q_out);
				bp.connect(trim, 0, even, 0);
				TurnIntoModule one_2 = new TurnIntoModule("TurnOne", domain, domain, 1, Q_in, Q_out);
				bp.connect(f, 2, one_2, 0);
				BinaryApplyFunctionModule add = new BinaryApplyFunctionModule("Add", new NusmvNumbers.Addition(domain), Q_in, q_size, Q_out);
				bp.connect(f, 1, add, 0);
				bp.connect(one_2, 0, add, 1);
				FilterModule filter = new FilterModule("Filter", domain, Q_in, q_size, Q_out);
				bp.connect(even, 0, filter, 1);
				bp.connect(add, 0, filter, 0);
				CumulateModule sum_2 = new CumulateModule("Sum", new NusmvNumbers.Addition(domain), Q_in, Q_out);
				bp.connect(filter, 0, sum_2, 0);
				bp.add(one_1, sum_1, f, trim, even, one_2, add, filter);
				bp.setInput(one_1, 0, 0);
				bp.setOutput(sum_2, 0, 0);
			}
			else
			{
				return null;
			}
			/*
			if (!is_comparison)
			{
				return bp;
			}
			compareWithItself(bp, domain, domain, Q_in, q_size, Q_out, property, f, trim, even, one_2, add, filter);
			return bp;
			 */
		}
		if (query.compareTo(Q_SUM_OF_DOUBLES) == 0)
		{
			if (r.getInt(DOMAIN_SIZE) < 3)
			{
				// This query is only possible if domain contains number 2
				return null;
			}
			if (!is_comparison)
			{
				BeepBeepPipeline bp = new BeepBeepPipeline("SumOfDoubles", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, new ProcessorQueue[] {new ProcessorQueue("ou", "oc_0", "ob_0", 1, domain)});
				ForkModule f = new ForkModule("Fork2", domain, 2, 1);
				BinaryApplyFunctionModule mul = new BinaryApplyFunctionModule("Mul", new NusmvNumbers.Multiplication(domain), Q_in, q_size, Q_out);
				TurnIntoModule two = new TurnIntoModule("TurnTwo", domain, domain, 2, Q_in, Q_out);
				bp.connect(f, 0, mul, 0);
				bp.connect(f, 1, two, 0);
				bp.connect(two, 0, mul, 1);
				CumulateModule sum = new CumulateModule("Sum", new NusmvNumbers.Addition(domain), Q_in, Q_out);
				bp.connect(mul, 0, sum, 0);
				bp.add(f, mul, two, sum);
				bp.setInput(f, 0, 0);
				bp.setOutput(sum, 0, 0);
				return bp;
			}
			else 
			{
				return null;
			}
		}
		if (query.compareTo(Q_WIN_SUM_OF_1) == 0)
		{
			// Window width is 3 if not specified
			c.x = c.x > 0 ? c.x : 3;
			if (!is_comparison)
			{
				BeepBeepPipeline bp = new BeepBeepPipeline("WindowSumOfOne", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, new ProcessorQueue[] {new ProcessorQueue("ou", "oc_0", "ob_0", 1, domain)});
				TurnIntoModule one = new TurnIntoModule("TurnOne", domain, domain, 1, Q_in, Q_out);
				CumulateModule sum = new CumulateModule("Sum1", new NusmvNumbers.Addition(domain), Q_in, Q_out);
				bp.connect(one, 0, sum, 0);
				CumulateModule add = new CumulateModule("Sum2", new NusmvNumbers.Addition(domain), c.x, c.x);
				WindowModule win = new WindowModule("Win", add, c.x, domain, domain, Q_in, Q_out);
				bp.connect(sum, 0, win, 0);
				bp.add(win, sum);
				bp.setInput(sum, 0, 0);
				bp.setOutput(win, 0, 0);
				return bp;
			}
			else
			{
				return null;
			}
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
			if (!is_comparison)
			{
				BeepBeepPipeline bp = new BeepBeepPipeline("OutputIfSmallerThanK", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, new ProcessorQueue[] {new ProcessorQueue("ou", "oc_0", "ob_0", 1, domain)});
				ForkModule f = new ForkModule("Fork3", domain, 3, 1);
				FilterModule filter = new FilterModule("Filter", domain, Q_in, q_size, Q_out);
				bp.connect(f, 0, filter, 0);
				TurnIntoModule turn_k = new TurnIntoModule("TurnK", domain, domain, c.x, Q_in, Q_out);
				bp.connect(f, 1, turn_k, 0);
				TurnIntoModule turn_1 = new TurnIntoModule("TurnOne", domain, domain, 1, Q_in, Q_out);
				bp.connect(f, 2, turn_1, 0);
				CumulateModule sum = new CumulateModule("Sum", new NusmvNumbers.Addition(domain), Q_in, Q_out);
				bp.connect(turn_1, 0, sum, 0);
				BinaryApplyFunctionModule gt = new BinaryApplyFunctionModule("Greater", new NusmvNumbers.IsLessOrEqual(domain), Q_in, q_size, Q_out);
				bp.connect(turn_k, 0, gt, 1);
				bp.connect(sum, 0, gt, 0);
				bp.connect(gt, 0, filter, 1);
				bp.add(f, filter, turn_k, turn_1, sum, gt);
				bp.setInput(f, 0, 0);
				bp.setOutput(filter, 0, 0);
				return bp;
			}
			else
			{
				return null;
			}
		}
		if (query.compareTo(Q_COMPARE_WINDOW_SUM_3) == 0)
		{
			ProcessorQueue[] out_queues;
			if (is_stepwise)
			{
				out_queues = new ProcessorQueue[] {new ProcessorQueue("ou0", "oc_0", "ob_0", 1, domain), new ProcessorQueue("ou1", "oc_1", "ob_1", 1, domain)};
			}
			else
			{
				out_queues = new ProcessorQueue[] {new ProcessorQueue("ou0", "oc_0", "ob_0", 1, BooleanDomain.instance)};
			}
			BeepBeepPipeline bp = new BeepBeepPipeline("CompareWindowSumThree", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, out_queues);
			ForkModule compare_fork = new ForkModule("Fork2", domain, 2, Q_in);
			bp.add(compare_fork);
			bp.setInput(compare_fork, 0, 0);
			ProcessorModule out_1 = null, out_2 = null;
			GroupModule g1 = new GroupModule("Group1", 1, new Domain[] {domain}, 1, new Domain[] {domain}, Q_in, Q_out);
			{
				int width = 3;
				CumulateModule add = new CumulateModule("Sum", new NusmvNumbers.Addition(domain), width, width);
				WindowModule win = new WindowModule("Win", add, width, domain, domain, Q_in, Q_out);
				g1.add(win);
				g1.associateInput(0, win, 0);
				g1.associateOutput(0, win, 0);
			}
			out_1 = g1;
			bp.connect(compare_fork, 0, g1, 0);
			GroupModule g2 = new GroupModule("Group2", 1, new Domain[] {domain}, 1, new Domain[] {domain}, Q_in, Q_out);
			{
				ForkModule f = new ForkModule("Fork3", domain, 3, Q_in);
				TrimModule trim1 = new TrimModule("Trim1", 1, domain, Q_in);
				BinaryApplyFunctionModule add1 = new BinaryApplyFunctionModule("Add", new NusmvNumbers.Addition(domain), Q_in, q_size, Q_out);
				g2.connect(f, 0, add1, 0);
				g2.connect(f, 1, trim1, 0);
				g2.connect(trim1, 0, add1, 1);
				TrimModule trim2 = new TrimModule("Trim2", 2, domain, Q_in);
				BinaryApplyFunctionModule add2 = new BinaryApplyFunctionModule("Add", new NusmvNumbers.Addition(domain), Q_in, q_size, Q_out);
				g2.connect(f, 2, trim2, 0);
				g2.connect(add1, 0, add2, 0);
				g2.connect(trim2, 0, add2, 1);
				g2.add(f, trim1, add1, trim2, add2);
				g2.associateInput(0, f, 0);
				g2.associateOutput(0, add2, 0);
			}
			out_2 = g2;
			bp.connect(compare_fork, 1, g2, 0);
			if (!is_stepwise)
			{
				BinaryApplyFunctionModule comp_eq = new BinaryApplyFunctionModule("Eq", new NusmvNumbers.IsEqual(domain), Q_in, q_size, Q_out);
				bp.add(comp_eq);
				bp.connect(out_1, 0, comp_eq, 0);
				bp.connect(out_2, 0, comp_eq, 1);
				bp.setOutput(comp_eq, 0, 0);
			}
			else
			{
				bp.setOutput(out_1, 0, 0);
				bp.setOutput(out_2, 0, 1);
			}
			return bp;
		}
		if (query.compareTo(Q_COMPARE_WINDOW_SUM_2) == 0)
		{
			ProcessorQueue[] out_queues;
			if (is_stepwise)
			{
				out_queues = new ProcessorQueue[] {new ProcessorQueue("ou0", "oc_0", "ob_0", 1, domain), new ProcessorQueue("ou1", "oc_1", "ob_1", 1, domain)};
			}
			else
			{
				out_queues = new ProcessorQueue[] {new ProcessorQueue("ou0", "oc_0", "ob_0", 1, BooleanDomain.instance)};
			}
			BeepBeepPipeline bp = new BeepBeepPipeline("CompareWindowSumTwo", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, out_queues);
			ForkModule compare_fork = new ForkModule("Fork2", domain, 2, Q_in);
			bp.add(compare_fork);
			bp.setInput(compare_fork, 0, 0);
			ProcessorModule out_1 = null, out_2 = null;
			GroupModule g1 = new GroupModule("Group1", 1, new Domain[] {domain}, 1, new Domain[] {domain}, Q_in, Q_out);
			{
				int width = 3;
				CumulateModule add = new CumulateModule("Sum", new NusmvNumbers.Addition(domain), width, width);
				WindowModule win = new WindowModule("Win", add, width, domain, domain, Q_in, Q_out);
				g1.add(win);
				g1.associateInput(0, win, 0);
				g1.associateOutput(0, win, 0);
			}
			out_1 = g1;
			bp.connect(compare_fork, 0, g1, 0);
			GroupModule g2 = new GroupModule("Group2", 1, new Domain[] {domain}, 1, new Domain[] {domain}, Q_in, Q_out);
			{
				ForkModule f = new ForkModule("Fork2", domain, 2, Q_in);
				TrimModule trim1 = new TrimModule("Trim1", 1, domain, Q_in);
				BinaryApplyFunctionModule add1 = new BinaryApplyFunctionModule("Add", new NusmvNumbers.Addition(domain), Q_in, q_size, Q_out);
				g2.connect(f, 0, add1, 0);
				g2.connect(f, 1, trim1, 0);
				g2.connect(trim1, 0, add1, 1);
				g2.add(f, trim1, add1);
				g2.associateInput(0, f, 0);
				g2.associateOutput(0, add1, 0);
			}
			out_2 = g2;
			bp.connect(compare_fork, 1, g2, 0);
			if (!is_stepwise)
			{
				BinaryApplyFunctionModule comp_eq = new BinaryApplyFunctionModule("Eq", new NusmvNumbers.IsEqual(domain), Q_in, q_size, Q_out);
				bp.add(comp_eq);
				bp.connect(out_1, 0, comp_eq, 0);
				bp.connect(out_2, 0, comp_eq, 1);
				bp.setOutput(comp_eq, 0, 0);
			}
			else
			{
				bp.setOutput(out_1, 0, 0);
				bp.setOutput(out_2, 0, 1);
			}
			return bp;
		}
		if (query.compareTo(Q_COMPARE_PASSTHROUGH_DELAY) == 0)
		{
			ProcessorQueue[] out_queues;
			if (is_stepwise)
			{
				out_queues = new ProcessorQueue[] {new ProcessorQueue("ou0", "oc_0", "ob_0", 1, domain), new ProcessorQueue("ou1", "oc_1", "ob_1", 1, domain)};
			}
			else
			{
				out_queues = new ProcessorQueue[] {new ProcessorQueue("ou0", "oc_0", "ob_0", 1, BooleanDomain.instance)};
			}
			BeepBeepPipeline bp = new BeepBeepPipeline("ComparePassthroughDelay", new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, out_queues);
			ForkModule compare_fork = new ForkModule("Fork2", domain, 2, Q_in);
			bp.add(compare_fork);
			bp.setInput(compare_fork, 0, 0);
			ProcessorModule out_1 = null, out_2 = null;
			PassthroughModule g1 = new PassthroughModule("pt", domain, Q_in);
			out_1 = g1;
			bp.connect(compare_fork, 0, g1, 0);
			GroupModule g2 = new GroupModule("Group2", 1, new Domain[] {domain}, 1, new Domain[] {domain}, Q_in, Q_out);
			{
				ForkModule f = new ForkModule("Fork2", domain, 2, Q_in);
				TrimModule trim = new TrimModule("Trim1", 1, domain, Q_in);
				g2.connect(f, 1, trim, 0);
				TurnIntoModule t = new TurnIntoModule("TurnTrue", domain, BooleanDomain.instance, true, Q_in, Q_out);
				g2.connect(trim, 0, t, 0);
				FilterModule filter = new FilterModule("Filter", domain, Q_in, q_size, Q_out);
				g2.connect(f, 0, filter, 0);
				g2.connect(t, 0, filter, 1);
				g2.add(f, trim, t, filter);
				g2.associateInput(0, f, 0);
				g2.associateOutput(0, filter, 0);
			}
			out_2 = g2;
			bp.connect(compare_fork, 1, g2, 0);
			if (!is_stepwise)
			{
				BinaryApplyFunctionModule comp_eq = new BinaryApplyFunctionModule("Eq", new NusmvNumbers.IsEqual(domain), Q_in, q_size, Q_out);
				bp.add(comp_eq);
				bp.connect(out_1, 0, comp_eq, 0);
				bp.connect(out_2, 0, comp_eq, 1);
				bp.setOutput(comp_eq, 0, 0);
			}
			else
			{
				bp.setOutput(out_1, 0, 0);
				bp.setOutput(out_2, 0, 1);
			}
			return bp;
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
		if (query.compareTo(Q_SUM_OF_ODDS) == 0)
		{
			return "/resource/SumOfOdds.png";
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
		if (query.compareTo(Q_COMPARE_PASSTHROUGH_DELAY) == 0)
		{
			return "/resource/ComparePassthroughDelay_k.png";
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
