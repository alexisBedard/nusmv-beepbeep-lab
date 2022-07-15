/*
  A benchmark for NuSMV extensions to BeepBeep 3
  Copyright (C) 2021-2022 Alexis Bédard and Sylvain Hallé

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
import java.util.Map;
import java.util.Set;

import ca.uqac.lif.cep.nusmv.BeepBeepModel;
import ca.uqac.lif.cep.nusmv.QueueOutOfBoundsException;
import ca.uqac.lif.nusmv4j.ArrayVariable;
import ca.uqac.lif.nusmv4j.Module;
import ca.uqac.lif.nusmv4j.ModuleDomain;
import ca.uqac.lif.nusmv4j.PrettyPrintStream;
import ca.uqac.lif.nusmv4j.ScalarVariable;
import ca.uqac.lif.nusmv4j.Variable;

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
	 * The NuSMV file corresponding to this model.
	 */
	protected BeepBeepModel m_pipeline;

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
	 * An internal URL to the picture that represents the processor chain.
	 */
	protected transient String m_imageUrl = null;

	/**
	 * Creates a new instance of model provider.
	 * @param start The pipeline corresponding to this model
	 * @param name A (textual) name given to the model in question 
	 * @param queue_size The size of the queues in the SMV model to generate
	 * @param domain_size The size of the domains in the SMV model to generate
	 * @param k Some processor chains may contain a processor
	 * that requires a parameter (such as a window width or a decimation
	 * interval); this additional value is represented by this abstract
	 * parameter.
	 * @param image_url An URL corresponding to the image for that processor
	 * chain. Set to <tt>null</tt> if no image is available.
	 */
	public BeepBeepModelProvider(BeepBeepModel start, String name, int queue_size, int domain_size, int k, String image_url) throws QueueOutOfBoundsException
	{
		super(name, queue_size, domain_size);
		m_parameter = k;
		m_pipeline = start;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrettyPrintStream ps = new PrettyPrintStream(baos);
		//System.out.println(name);
		m_pipeline.print(ps);
		m_fileContents = baos.toString();
		m_modules = start.getModules();
		m_imageUrl = image_url;
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
		e.writeOutput(NUM_MODULES, m_modules.size());
		e.writeOutput(GENERATION_TIME, m_generationTime);
		e.writeOutput(NUM_VARIABLES, countVariables());
		e.describe(QUEUE_VARIABLES, "The number of variables in the model corresponding to queues");
		e.writeOutput(QUEUE_VARIABLES, getQueueVariables().size());
		if (m_parameter > 0)
		{
			e.describe(K, "The value of parameter k in the processor chain");
			e.writeInput(K, m_parameter);
		}
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
		Set<Variable> vars = new HashSet<Variable>();
		for (Module m : m_modules)
		{
			if (m.getName().compareTo("main") == 0)
			{
				fetchAllVariables(m, "", vars);
			}
		}
		return vars.size();
	}

	/**
	 * Gets the URL associated to the picture for this processor chain.
	 * @return The URL, or <tt>null</tt> if no image exists
	 */
	public String getImageUrl()
	{
		return m_imageUrl;
	}

	/**
	 * Recursively fetches the variables in a hierarchy of SMV modules.
	 * @param m The current module to examine
	 * @param prefix The prefix given to the variables (non-empty if m is a
	 * nested module)
	 * @param vars The set where variables are to be added 
	 */
	protected static void fetchAllVariables(Module m, String prefix, Set<Variable> vars)
	{
		for (Variable v : m.getVariables())
		{
			String var_name = prefix + m.getName() + "." + v.getName();
			vars.add(duplicateVariable(var_name, v));
		}
		for (Map.Entry<String,ModuleDomain> e : m.getSubModules().entrySet())
		{
			fetchAllVariables(e.getValue().getModule(), prefix + e.getKey() + ".", vars);
		}
	}

	/**
	 * Gets all the variables inside this model that correspond to queue
	 * flags.
	 * @return The set of queue variables
	 */
	public Set<ArrayVariable> getQueueVariables()
	{
		if (m_modules == null)
		{
			return new HashSet<ArrayVariable>(0);
		}
		return getQueueVariables(m_modules);
	}
	
	/**
	 * Gets all the variables inside this model that correspond to queue
	 * flags.
	 * @return The set of queue variables
	 */
	public Set<Integer> getInputPipeIds()
	{
		Set<Integer> vars = new HashSet<Integer>();
		if (m_modules == null)
		{
			return new HashSet<Integer>(0);
		}
		for (Module m : m_modules)
		{
			if (m.getName().compareTo("main") == 0)
			{
				for (Variable v : m.getVariables())
				{
					String v_name = v.getName();
					if (v_name.startsWith("inb"))
					{
						int id = Integer.parseInt(v_name.substring(4));
						vars.add(id);
					}
				}
				break;
			}
		}
		return vars;
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
		for (Module m : m_modules)
		{
			if (m.getName().compareTo("main") == 0)
			{
				for (Variable v : m.getVariables())
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
	 * Gets the number of processors in the pipeline modeled in this file.
	 * @return The number of processors
	 */
	public int getNumProcessors()
	{
		return m_numProcessors;
	}

	/**
	 * Gets all the variables inside a set of modules that correspond to queue
	 * flags.
	 * @param modules The set of modules
	 * @return The set of queue variables
	 */
	public static Set<ArrayVariable> getQueueVariables(Set<Module> modules)
	{
		Set<ArrayVariable> queue_variables = new HashSet<ArrayVariable>();
		for (Module m : modules)
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
	protected static void fetchQueueVariables(Module m, String prefix, Set<ArrayVariable> queue_variables)
	{
		for (Variable v : m.getVariables())
		{
			if (v instanceof ArrayVariable && v.getName().startsWith("bfb_"))
			{
				ArrayVariable av = (ArrayVariable) v;
				queue_variables.add(new ArrayVariable(prefix + av.getName(), av.getDomain(), av.getDimension()));
			}
		}
		for (Map.Entry<String,ModuleDomain> e : m.getSubModules().entrySet())
		{
			ModuleDomain md = e.getValue();
			fetchQueueVariables((Module) md.getModule(), prefix + e.getKey() + ".", queue_variables);
		}
	}
	
	
	/*@ non_null @*/ protected static Variable duplicateVariable(String new_name, Variable v)
	{
		if (v instanceof ScalarVariable)
		{
			return new ScalarVariable(new_name, v.getDomain());
		}
		if (v instanceof ArrayVariable)
		{
			return new ArrayVariable(new_name, v.getDomain(), ((ArrayVariable) v).getDimension());
		}
		throw new RuntimeException("Unknown variable type");
	}
}
