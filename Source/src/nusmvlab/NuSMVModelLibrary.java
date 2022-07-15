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
import ca.uqac.lif.cep.nusmv.FilterModule;
import ca.uqac.lif.cep.nusmv.ForkModule;
import ca.uqac.lif.cep.nusmv.NusmvNumbers;
import ca.uqac.lif.cep.nusmv.PassthroughModule;
import ca.uqac.lif.cep.nusmv.ProcessorModule;
import ca.uqac.lif.cep.nusmv.ProcessorQueue;
import ca.uqac.lif.cep.nusmv.TrimModule;
import ca.uqac.lif.cep.nusmv.TurnIntoModule;
import ca.uqac.lif.cep.nusmv.UnaryApplyFunctionModule;
import ca.uqac.lif.cep.nusmv.WindowModule;
import ca.uqac.lif.labpal.region.Point;
import ca.uqac.lif.labpal.region.Region;
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
	public ModelProvider get(Point p)
	{
		String query = p.getString(QUERY);
		int domain_size = p.getInt(DOMAIN_SIZE);
		int queue_size = p.getInt(QUEUE_SIZE);
		Count c = new Count();
		c.x = -1;
		Object o_k = p.get(K);
		if (o_k instanceof Number)
		{
			c.x = ((Number) o_k).intValue();
		}
		if (query.compareTo(Q_DUMMY) == 0)
		{
			return new DummyModelProvider(queue_size, domain_size);
		}
		ModelId m = new ModelId(p);
		BeepBeepPipeline start = null;
		if (m_cache.containsKey(m)) 
		{
			start = m_cache.get(m);
		}
		else
		{
			start = getProcessorChain(p, c);
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
	 * @param p The point corresponding to the chain to create
	 * @param query The name of the chain to create
	 * @return A reference to the first processor of the chain
	 */
	protected static BeepBeepPipeline getProcessorChain(Point r, Count c)
	{
		String property = r.getString(PROPERTY);
		int dom_size = r.getInt(DOMAIN_SIZE);
		Domain domain = new IntegerRange(0, dom_size);
		int q_size = r.getInt(QUEUE_SIZE);
		boolean is_comparison = property.compareTo(OutputsAlwaysEqual.NAME) == 0 || property.compareTo(OutputAlwaysTrue.NAME) == 0;
		boolean is_stepwise = property.compareTo(OutputsAlwaysEqual.NAME) == 0;
		int Q_in = 1, Q_out = 1;
		PipelineCreatorPair pcp = getPipelineCreators(r, c);
		if (pcp == null)
		{
			return null;
		}
		PipelineCreator pc1 = pcp.pc1;
		PipelineCreator pc2 = pcp.pc2;
		String pipeline_name = pcp.pipeline_name;
		if (!is_comparison)
		{
			BeepBeepPipeline bp = new BeepBeepPipeline(pipeline_name, new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, new ProcessorQueue[] {new ProcessorQueue("o", "oc_0", "ob_0", 1, domain)});
			ProcessorModule[] ports = pc1.get(bp, domain, Q_in, q_size, Q_out, c);
			bp.setInput(ports[0], 0, 0);
			bp.setOutput(ports[1], 0, 0);
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
			BeepBeepPipeline bp = new BeepBeepPipeline(pipeline_name, new ProcessorQueue[] {new ProcessorQueue("in", "inc_0", "inb_0", 1, domain)}, out_queues);
			ForkModule compare_fork = new ForkModule("Fork2", domain, 2, Q_in);
			bp.add(compare_fork);
			bp.setInput(compare_fork, 0, 0);
			ProcessorModule[] ports1 = pc1.get(bp, domain, Q_in, q_size, Q_out, c);
			bp.connect(compare_fork, 0, ports1[0], 0);
			ProcessorModule[] ports2 = pc2.get(bp, domain, Q_in, q_size, Q_out, c);
			bp.connect(compare_fork, 1, ports2[0], 0);
			if (!is_stepwise)
			{
				BinaryApplyFunctionModule comp_eq = new BinaryApplyFunctionModule("Eq", new NusmvNumbers.IsEqual(domain), Q_in, q_size, Q_out);
				bp.add(comp_eq);
				bp.connect(ports1[1], 0, comp_eq, 0);
				bp.connect(ports2[1], 0, comp_eq, 1);
				bp.setOutput(comp_eq, 0, 0);
			}
			else
			{
				bp.setOutput(ports1[1], 0, 0);
				bp.setOutput(ports2[1], 0, 1);
			}
			return bp;
		}
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
	
	protected static PipelineCreatorPair getPipelineCreators(Point r, Count c)
	{
		String query = r.getString(QUERY);
		PipelineCreator pc1 = null, pc2 = null;
		String pipeline_name = "";
		if (query.compareTo(Q_PASSTHROUGH) == 0)
		{
			pc1 = new CreatePassthrough();
			pc2 = new CreatePassthrough();
			pipeline_name = "Passthrough";
		}
		else if (query.compareTo(Q_PRODUCT) == 0)
		{
			pc1 = new CreateProduct();
			pc2 = new CreateProduct();
			pipeline_name = "Product";
		}
		else if (query.compareTo(Q_PRODUCT_1_K) == 0)
		{
			// Decimation interval is 3 if not specified
			c.x = c.x > 0 ? c.x : 3;
			pc1 = new CreateProductOneK();
			pc2 = new CreateProductOneK();
			pipeline_name = "ProductOneK";
		}
		else if (query.compareTo(Q_PRODUCT_WINDOW_K) == 0)
		{
			// Window width is 3 if not specified
			c.x = c.x > 0 ? c.x : 3;
			pc1 = new CreateProductWindowK();
			pc2 = new CreateProductWindowK();
			pipeline_name = "ProductWindowK";
		}
		else if (query.compareTo(Q_SUM_OF_ODDS) == 0)
		{
			pc1 = new CreateSumOfOdds();
			pc2 = new CreateSumOfOdds();
			pipeline_name = "SumOfOdds";
		}
		else if (query.compareTo(Q_SUM_OF_DOUBLES) == 0)
		{
			if (r.getInt(DOMAIN_SIZE) < 3)
			{
				// This query is only possible if domain contains number 2
				return null;
			}
			pc1 = new CreateSumOfDoubles();
			pc2 = new CreateSumOfDoubles();
			pipeline_name = "SumOfDoubles";
		}
		else if (query.compareTo(Q_WIN_SUM_OF_1) == 0)
		{
			// Window width is 3 if not specified
			c.x = c.x > 0 ? c.x : 3;
			pc1 = new CreateWinSumOfOne();
			pc2 = new CreateWinSumOfOne();
			pipeline_name = "WindowSumOfOne";
		}
		else if (query.compareTo(Q_OUTPUT_IF_SMALLER_K) == 0)
		{
			// Parameter value is 3 if not specified
			c.x = c.x > 0 ? c.x : 3;
			if (r.getInt(DOMAIN_SIZE) <= c.x)
			{
				// This query is only possible if domain contains number k
				return null;
			}
			pc1 = new CreateOutputIfSmallerThanK();
			pc2 = new CreateOutputIfSmallerThanK();
			pipeline_name = "OutputIfSmallerThanK";
		}
		else if (query.compareTo(Q_COMPARE_WINDOW_SUM_3) == 0)
		{
			pc1 = new CreateCompareWindowSum3a();
			pc2 = new CreateCompareWindowSum3b();
			pipeline_name = "CompareWindowSumThree";
		}
		else if (query.compareTo(Q_COMPARE_WINDOW_SUM_2) == 0)
		{
			pc1 = new CreateCompareWindowSum2a();
			pc2 = new CreateCompareWindowSum2b();
			pipeline_name = "CompareWindowSumTwo";
		}
		else if (query.compareTo(Q_COMPARE_PASSTHROUGH_DELAY) == 0)
		{
			pc1 = new CreatePassthrough();
			pc2 = new CreateFilterDelay();
			pipeline_name = "ComparePassthroughDelay";
		}
		return new PipelineCreatorPair(pc1, pc2, pipeline_name);
	}

	protected interface PipelineCreator
	{
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c);
	}

	protected static class CreatePassthrough implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
			PassthroughModule pt = new PassthroughModule("pt", domain, Q_in);
			bp.add(pt);
			return new ProcessorModule[] {pt, pt};
		}
	}
	
	protected static class CreateProduct implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
			CumulateModule prod = new CumulateModule("prod", new NusmvNumbers.Multiplication(domain), Q_in, Q_out);
			bp.add(prod);
			return new ProcessorModule[] {prod, prod};
		}
	}
	
	protected static class CreateProductOneK implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
			ForkModule f = new ForkModule("Fork2", domain, 2, Q_in);
			BinaryApplyFunctionModule mul = new BinaryApplyFunctionModule("Mul", new NusmvNumbers.Multiplication(domain), Q_in, q_size, Q_out);
			CountDecimateModule dec = new CountDecimateModule("Decimate" + c.x, c.x, domain, Q_in, Q_out);
			bp.connect(f, 0, mul, 0);
			bp.connect(f, 1, dec, 0);
			bp.connect(dec, 0, mul, 1);
			bp.add(f, mul, dec);
			return new ProcessorModule[] {f, mul};
		}
	}
	
	protected static class CreateProductWindowK implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
			CumulateModule prod = new CumulateModule("Product", new NusmvNumbers.Multiplication(domain), c.x, c.x);
			WindowModule win = new WindowModule("Win", prod, c.x, domain, domain, Q_in, Q_out);
			bp.add(win);
			return new ProcessorModule[] {win, win};
		}
	}
	
	protected static class CreateSumOfOdds implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
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
			return new ProcessorModule[] {one_1, sum_2};
		}
	}
	
	protected static class CreateSumOfDoubles implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
			ForkModule f = new ForkModule("Fork2", domain, 2, 1);
			BinaryApplyFunctionModule mul = new BinaryApplyFunctionModule("Mul", new NusmvNumbers.Multiplication(domain), Q_in, q_size, Q_out);
			TurnIntoModule two = new TurnIntoModule("TurnTwo", domain, domain, 2, Q_in, Q_out);
			bp.connect(f, 0, mul, 0);
			bp.connect(f, 1, two, 0);
			bp.connect(two, 0, mul, 1);
			CumulateModule sum = new CumulateModule("Sum", new NusmvNumbers.Addition(domain), Q_in, Q_out);
			bp.connect(mul, 0, sum, 0);
			bp.add(f, mul, two, sum);
			return new ProcessorModule[] {f, sum};
		}
	}
	
	protected static class CreateWinSumOfOne implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
			TurnIntoModule one = new TurnIntoModule("TurnOne", domain, domain, 1, Q_in, Q_out);
			CumulateModule sum = new CumulateModule("Sum1", new NusmvNumbers.Addition(domain), Q_in, Q_out);
			bp.connect(one, 0, sum, 0);
			CumulateModule add = new CumulateModule("Sum2", new NusmvNumbers.Addition(domain), c.x, c.x);
			WindowModule win = new WindowModule("Win", add, c.x, domain, domain, Q_in, Q_out);
			bp.connect(sum, 0, win, 0);
			bp.add(win, sum);
			return new ProcessorModule[] {sum, win};
		}
	}
	
	protected static class CreateOutputIfSmallerThanK implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
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
			return new ProcessorModule[] {f, filter};
		}
	}
	
	protected static class CreateCompareWindowSum3a implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
			int width = 3;
			CumulateModule add = new CumulateModule("Sum", new NusmvNumbers.Addition(domain), width, width);
			WindowModule win = new WindowModule("Win", add, width, domain, domain, Q_in, Q_out);
			bp.add(win);
			return new ProcessorModule[] {win, win};
		}
	}
	
	protected static class CreateCompareWindowSum3b implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
			ForkModule f = new ForkModule("Fork3", domain, 3, Q_in);
			TrimModule trim1 = new TrimModule("Trim1", 1, domain, Q_in);
			BinaryApplyFunctionModule add1 = new BinaryApplyFunctionModule("Add", new NusmvNumbers.Addition(domain), Q_in, q_size, Q_out);
			bp.connect(f, 0, add1, 0);
			bp.connect(f, 1, trim1, 0);
			bp.connect(trim1, 0, add1, 1);
			TrimModule trim2 = new TrimModule("Trim2", 2, domain, Q_in);
			BinaryApplyFunctionModule add2 = new BinaryApplyFunctionModule("Add", new NusmvNumbers.Addition(domain), Q_in, q_size, Q_out);
			bp.connect(f, 2, trim2, 0);
			bp.connect(add1, 0, add2, 0);
			bp.connect(trim2, 0, add2, 1);
			bp.add(f, trim1, add1, trim2, add2);
			return new ProcessorModule[] {f, add2};
		}
	}
	
	protected static class CreateCompareWindowSum2a implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
			int width = 2;
			CumulateModule add = new CumulateModule("Sum", new NusmvNumbers.Addition(domain), width, width);
			WindowModule win = new WindowModule("Win", add, width, domain, domain, Q_in, Q_out);
			bp.add(win);
			return new ProcessorModule[] {win, win};
		}
	}
	
	protected static class CreateCompareWindowSum2b implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
			ForkModule f = new ForkModule("Fork2", domain, 2, Q_in);
			TrimModule trim1 = new TrimModule("Trim1", 1, domain, Q_in);
			BinaryApplyFunctionModule add1 = new BinaryApplyFunctionModule("Add", new NusmvNumbers.Addition(domain), Q_in, q_size, Q_out);
			bp.connect(f, 0, add1, 0);
			bp.connect(f, 1, trim1, 0);
			bp.connect(trim1, 0, add1, 1);
			bp.add(f, trim1, add1);
			return new ProcessorModule[] {f, add1};
		}
	}
	
	protected static class CreateFilterDelay implements PipelineCreator
	{
		@Override
		public ProcessorModule[] get(BeepBeepPipeline bp, Domain domain, int Q_in, int q_size, int Q_out, Count c)
		{
			ForkModule f = new ForkModule("Fork2", domain, 2, Q_in);
			TrimModule trim = new TrimModule("Trim1", 1, domain, Q_in);
			bp.connect(f, 1, trim, 0);
			TurnIntoModule t = new TurnIntoModule("TurnTrue", domain, BooleanDomain.instance, true, Q_in, Q_out);
			bp.connect(trim, 0, t, 0);
			FilterModule filter = new FilterModule("Filter", domain, Q_in, q_size, Q_out);
			bp.connect(f, 0, filter, 0);
			bp.connect(t, 0, filter, 1);
			bp.add(f, trim, t, filter);
			return new ProcessorModule[] {f, filter};
		}
	}
	
	protected static class PipelineCreatorPair
	{
		public PipelineCreator pc1;
		public PipelineCreator pc2;
		public String pipeline_name;
		
		public PipelineCreatorPair(PipelineCreator pc1, PipelineCreator pc2, String pipeline_name)
		{
			super();
			this.pc1 = pc1;
			this.pc2 = pc2;
			this.pipeline_name = pipeline_name;
		}
	}
}
