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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import ca.uqac.lif.cep.Processor;
import ca.uqac.lif.cep.smv.SmvCrawler;
import ca.uqac.lif.cep.smv.SmvModule;
import ca.uqac.lif.cep.smv.SmvModule.SmvVariable;

/**
 * Provides a NuSMV model based on a chain of BeepBeep processors.
 */
public class BeepBeepModelProvider extends ModelProvider
{
	/**
	 * Name of parameter "Number of processors".
	 */
	public static final transient String NUM_PROCESSORS = "Number of processors";
	
	/**
	 * Name of parameter "Queue variables".
	 */
	public static final transient String QUEUE_VARIABLES = "Queue variables";
	
	/**
	 * Name of parameter "k".
	 */
	public static final transient String K = "k";
	
	/**
	 * The contents of the SMV file to be printed.
	 */
	protected String m_fileContents;
	
	/**
	 * The processor at the start of the chain.
	 */
	protected Processor m_start;
	
	/**
	 * The value of the additional parameter that the processor chain can
	 * have.
	 */
	protected int m_parameter;
	
	/**
	 * The time taken to generate the model.
	 */
	protected long m_generationTime;
	
	/**
	 * The number of distinct processor instances inside this chain.
	 */
	protected int m_numProcessors;
	
	/**
	 * Creates a new instance of model provider.
	 * @param start The processor instance at the start of the chain
	 * @param name A (textual) name given to the model in question 
	 * @param queue_size The size of the queues in the SMV model to generate
	 * @param domain_size The size of the domains in the SMV model to generate
	 * @param k Some processor chains may contain a processor
	 * that requires a parameter (such as a window width or a decimation
	 * interval); this additional value is represented by this abstract
	 * parameter.
	 */
	public BeepBeepModelProvider(Processor start, String name, int queue_size, int domain_size, int k)
	{
		super(name, queue_size, domain_size);
		m_parameter = k;
		m_start = start;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		SmvCrawler sc = new SmvCrawler(ps, m_queueSize, m_domainSize);
		long t_start = System.currentTimeMillis();
		sc.crawl(m_start);
		long t_end = System.currentTimeMillis();
		m_fileContents = baos.toString();
		m_generationTime = t_end - t_start;
		m_modules = sc.getModules();
		
	}
	
	@Override
	public void printToFile(PrintStream ps) throws IOException
	{
		ps.print(m_fileContents);
	}
	
	@Override
	public void fillExperiment(NuSMVExperiment e)
	{
		super.fillExperiment(e);
		e.write(NUM_MODULES, m_modules.size());
		e.write(GENERATION_TIME, m_generationTime);
		e.write(NUM_VARIABLES, countVariables());
		e.describe(QUEUE_VARIABLES, "The number of variables in the model corresponding to queues");
		e.write(QUEUE_VARIABLES, getQueueVariables().size());
	}
	
	/**
	 * Counts all the variables in all the modules of the generated SMV model.
	 * @return The number of variables
	 */
	public int countVariables()
	{
		if (m_modules == null)
		{
			return 0;
		}
		Set<SmvVariable> vars = new HashSet<SmvVariable>();
		for (SmvModule m : m_modules)
		{
			if (m.getName().compareTo("main") == 0)
			{
				fetchAllVariables(m, "", vars);
			}
		}
		return vars.size();
	}
	
	/**
	 * Recursively fetches the variables in a hierarchy of SMV modules.
	 * @param m The current module to examine
	 * @param prefix The prefix given to the variables (non-empty if m is a
	 * nested module)
	 * @param vars The set where variables are to be added 
	 */
	protected static void fetchAllVariables(SmvModule m, String prefix, Set<SmvVariable> vars)
	{
		for (SmvVariable v : m.getVariables())
		{
			String var_name = prefix + v.getName() + "." + v.getName();
			vars.add(new SmvVariable(var_name, v.getType()));
			if (v.getType() instanceof SmvModule)
			{
				fetchAllVariables((SmvModule) v.getType(), var_name + ".", vars);
			}
		}
	}
	
	/**
	 * Gets all the variables inside this model that correspond to queue
	 * flags.
	 * @return The set of queue variables
	 */
	public Set<SmvVariable> getQueueVariables()
	{
		if (m_modules == null)
		{
			return new HashSet<SmvVariable>(0);
		}
		return getQueueVariables(m_modules);
	}
	
	/**
	 * Gets all the variables inside this model that correspond to queue
	 * flags.
	 * @return The set of queue variables
	 */
	public Set<Integer> getOutputPipeIds()
	{
		Set<Integer> vars = new HashSet<Integer>();
		if (m_modules == null)
		{
			return new HashSet<Integer>(0);
		}
		for (SmvModule m : m_modules)
		{
			if (m.getName().compareTo("main") == 0)
			{
				for (SmvVariable v : m.getVariables())
				{
					String v_name = v.getName();
					if (v_name.startsWith("oc"))
					{
						int id = Integer.parseInt(v_name.substring(3));
						vars.add(id);
					}
				}
				break;
			}
		}
		return vars;
	}

	/**
	 * Gets all the variables inside a set of modules that correspond to queue
	 * flags.
	 * @param modules The set of modules
	 * @return The set of queue variables
	 */
	public static Set<SmvVariable> getQueueVariables(Set<SmvModule> modules)
	{
		Set<SmvVariable> queue_variables = new HashSet<SmvVariable>();
		for (SmvModule m : modules)
		{
			if (m.getName().compareTo("main") == 0)
			{
				fetchQueueVariables(m, "", queue_variables);
			}
		}
		return queue_variables;
	}
	
	/**
	 * Recursively fetches the queue variables in a hierarchy of SMV modules.
	 * @param m The current module to examine
	 * @param prefix The prefix given to the variables (non-empty if m is a
	 * nested module)
	 * @param queue_variables The set where variables are to be added 
	 */
	protected static void fetchQueueVariables(SmvModule m, String prefix, Set<SmvVariable> queue_variables)
	{
		for (SmvVariable v : m.getVariables())
		{
			if (v.getName().startsWith("qb"))
			{
				queue_variables.add(new SmvVariable(prefix + v.getName(), v.getSize(), Boolean.class));
			}
			if (v.getType() instanceof SmvModule)
			{
				fetchQueueVariables((SmvModule) v.getType(), prefix + v.getName() + ".", queue_variables);
			}
		}
	}
}
