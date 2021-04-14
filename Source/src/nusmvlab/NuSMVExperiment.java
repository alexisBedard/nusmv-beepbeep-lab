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
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.uqac.lif.labpal.CommandRunner;
import ca.uqac.lif.labpal.Experiment;
import ca.uqac.lif.labpal.ExperimentException;
import ca.uqac.lif.labpal.FileHelper;
import nusmvlab.PropertyProvider.Logic;

/**
 * Experiment that runs NuSMV on a given input model, evaluates a CTL or LTL
 * property, and gathers statistics about the process.
 */
public class NuSMVExperiment extends Experiment
{
	/**
	 * The name of attribute "Time".
	 */
	public static final transient String TIME = "Time";

	/**
	 * The name of attribute "Memory".
	 */
	public static final transient String MEMORY = "Memory";

	/**
	 * The name of attribute "Total BDD nodes".
	 */
	public static final transient String TOTAL_NODES = "Total BDD nodes";

	/**
	 * The name of attribute "Live BDD nodes".
	 */
	public static final transient String LIVE_NODES = "Live BDD nodes";
	
	/**
	 * The name of attribute "Verdict".
	 */
	public static final transient String VERDICT = "Verdict";
	
	/**
	 * The name of attribute "Witness length".
	 */
	public static final transient String WITNESS_LENGTH = "Witness length";
	
	/**
	 * The name of attribute "Reachable states".
	 */
	public static final transient String REACHABLE_STATES = "Reachable states";
	
	/**
	 * The name of attribute "Total states".
	 */
	public static final transient String TOTAL_STATES = "Total states";
	/**
	 * The name of attribute "System diameter".
	 */
	public static final transient String SYSTEM_DIAMETER = "System diameter";

	/**
	 * The command to call to run NuSMV from the command line.
	 */
	public static final transient String NUSMV_PATH = "NuSMV";

	/**
	 * The name of the OS's temporary directory.
	 */
	protected static final transient String TMP_DIR = System.getProperty("java.io.tmpdir");

	/**
	 * The OS-dependent file separator character.
	 */
	protected static final transient String FILE_SEPARATOR = System.getProperty("file.separator");

	/**
	 * The regex pattern to read memory consumption from NuSMV's output
	 */
	protected static final transient Pattern s_memoryPattern = Pattern.compile("Memory in use: (\\d+)");

	/**
	 * The regex pattern to read total nodes from NuSMV's output
	 */
	protected static final transient Pattern s_totalNodesPattern = Pattern.compile("Peak number of nodes: (\\d+)");

	/**
	 * The regex pattern to read live nodes from NuSMV's output
	 */
	protected static final transient Pattern s_liveNodesPattern = Pattern.compile("Peak number of live nodes: (\\d+)");
	
	/**
	 * The regex pattern to identify states of a counter-example trace
	 */
	protected static final transient Pattern s_witnessPattern = Pattern.compile("State: (\\d+)\\.(\\d+)");
	
	/**
	 * The regex pattern to identify system diameter
	 */
	protected static final transient Pattern s_diameterPattern = Pattern.compile("system diameter: (\\d+)");
	
	/**
	 * The regex pattern to identify system diameter
	 */
	protected static final transient Pattern s_reachableStatesPattern = Pattern.compile("reachable states: .*?\\^([\\d\\.]+)");
	
	/**
	 * The regex pattern to identify system diameter
	 */
	protected static final transient Pattern s_totalStatesPattern = Pattern.compile("out of .*?\\^([\\d\\.]+)");

	/**
	 * An object that provides a NuSMV model to the experiment.
	 */
	protected transient ModelProvider m_modelProvider;

	/**
	 * An object that provides a CTL/LTL property to the experiment.
	 */
	protected transient PropertyProvider m_propertyProvider;

	/**
	 * Creates a new instance of NuSMVExperiment.
	 * @param model  An object that provides a NuSMV file to the experiment
	 */
	public NuSMVExperiment(/*@ non_null @*/ ModelProvider model, PropertyProvider property)
	{
		super();
		describe(TIME, "The time (in ms) taken to process the NuSMV model");
		describe(MEMORY, "Total memory (in bytes) used by NuSMV");
		describe(TOTAL_NODES, "Peak number of BDD nodes");
		describe(LIVE_NODES, "Peak number of live BDD nodes");
		describe(VERDICT, "The verdict calculated by NuSMV on the evaluation of the property");
		describe(WITNESS_LENGTH, "The length of the counter-example trace if one is provided");
		describe(REACHABLE_STATES, "The base-2 logarithm of the number of reachable states in the NuSMV model");
		describe(TOTAL_STATES, "The base-2 logarithm of the total number of states in the NuSMV model");
		describe(SYSTEM_DIAMETER, "The system diameter of the NuSMV model");
		m_modelProvider = model;
		m_propertyProvider = property;
		m_modelProvider.fillExperiment(this);
		m_propertyProvider.fillExperiment(this);
	}

	@Override
	public void execute() throws ExperimentException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = null;
		try
		{
			ps = new PrintStream(baos, true, "UTF-8");
			printModel(ps);
			ps.close();
		}
		catch (IOException e)
		{
			throw new ExperimentException(e);
		}
		String model = baos.toString();
		long start_time = System.currentTimeMillis();
		String output = runNuSMV(model, getSourceCheckFilename());
		long end_time = System.currentTimeMillis();
		parseCheckResults(output);
		write(TIME, end_time - start_time);
		output = runNuSMV(model, getSourceStatsFilename());
		parseStatsResults(output);
	}

	/**
	 * Prints the complete NuSMV model of this experiment.
	 * @param ps The print stream where the model is to be printed 
	 * @throws IOException Thrown if printing the model did not succeed
	 */
	public void printModel(PrintStream ps) throws IOException
	{
		m_modelProvider.printToFile(ps);
		ps.println();
		if (m_propertyProvider.getLogic() == Logic.CTL)
		{
			ps.println("CTLSPEC");
		}
		else
		{
			ps.println("LTLSPEC");
		}
		m_propertyProvider.printToFile(ps);
	}

	/**
	 * Runs NuSMV on a model file. The model file is first written to an external
	 * file, after which NuSMV is called and its output is parsed to extract some
	 * data about its execution.
	 * @param model The model to process with NuSMV
	 * @param source_filename
	 * @throws ExperimentException Thrown if the call to NuSMV did not succeed
	 * for some reason
	 */
	protected String runNuSMV(String model, String source_filename) throws ExperimentException
	{
		CommandRunner runner = getRunner(false, model, source_filename);
		runner.run();
		byte[] bytes = runner.getBytes();
		if (bytes == null)
		{
			throw new ExperimentException("The call to NuSMV returned null");
		}
		String output = new String(bytes);
		int outcode = runner.getErrorCode();
		if (outcode != 0)
		{
			throw new ExperimentException("NuSMV existed with code " + outcode);
		}
		return output;
	}
	
	/**
	 * Parses the results output by NuSMV and fills experiment parameters,
	 * for the property checking part.
	 * @param output The string containing the standard output as written to by
	 * NuSMV
	 */
	protected void parseCheckResults(String output)
	{
		if (output.contains("is false"))
		{
			write(VERDICT, "False");
		}
		if (output.contains("is true"))
		{
			write(VERDICT, "True");
		}
		int w_len = 0;
		Matcher mat = s_witnessPattern.matcher(output);
		while (mat.find())
		{
			w_len++;
		}
		write(WITNESS_LENGTH, w_len);
	}
	
	/**
	 * Parses the results output by NuSMV and fills experiment parameters,
	 * for the stats gathering part.
	 * @param output The string containing the standard output as written to by
	 * NuSMV
	 */
	protected void parseStatsResults(String output)
	{
		write(MEMORY, readIntFromOutput(output, s_memoryPattern));
		write(TOTAL_NODES, readIntFromOutput(output, s_totalNodesPattern));
		write(LIVE_NODES, readIntFromOutput(output, s_liveNodesPattern));
		write(SYSTEM_DIAMETER, readIntFromOutput(output, s_diameterPattern));
		write(TOTAL_STATES, readFloatFromOutput(output, s_totalStatesPattern));
		write(REACHABLE_STATES, readFloatFromOutput(output, s_reachableStatesPattern));
	}

	/**
	 * Extracts an integer number from a regex expression.
	 * @param output The string where to apply the regex
	 * @param pat The pattern to look for
	 * @return The integer parsed from the pattern
	 */
	protected int readIntFromOutput(String output, Pattern pat)
	{
		Matcher mat = pat.matcher(output);
		if (!mat.find())
		{
			return -1;
		}
		return Integer.parseInt(mat.group(1));
	}
	
	/**
	 * Extracts a float from a regex expression.
	 * @param output The string where to apply the regex
	 * @param pat The pattern to look for
	 * @return The float parsed from the pattern
	 */
	protected float readFloatFromOutput(String output, Pattern pat)
	{
		Matcher mat = pat.matcher(output);
		if (!mat.find())
		{
			return -1;
		}
		return Float.parseFloat(mat.group(1));
	}

	/**
	 * Called by the factory to notify the experiment that an ID has been
	 * assigned to it. This method circumvents the fact that an experiment does
	 * not yet know its ID when its constructor is called.
	 * @param id The experiment's id
	 */
	public void tellId(int id)
	{
		writeDescription(id);
	}

	/**
	 * Prepares the description text to be added to each experiment.
	 * @param id The experiment's id
	 */
	protected void writeDescription(int id)
	{
		StringBuilder out = new StringBuilder();
		out.append("<p>Experiment that turns a BeepBeep chain of processors into a NuSMV model, and verifies a CTL or LTL property on this model.</p>\n");
		String image_url = ((BeepBeepModelProvider) m_modelProvider).getImageUrl();
		if (image_url != null)
		{
			out.append("<img src=\"" + image_url + "\" alt=\"Processor chain\" />\n");
		}
		out.append("<p><a href=\"/view-model?id=" + id + "\">View the SMV model file</a></p>");
		out.append("<p>The property to evaluate is:</p>\n");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		try
		{
			m_propertyProvider.printToFile(ps);
		}
		catch (IOException e)
		{
			// Do nothing in such a case
		}
		out.append("<blockquote>").append(baos.toString()).append("</blockquote>\n");
		setDescription(out.toString());
	}

	/**
	 * Gets an instance of {@link CommandRunner} that calls NuSMV on the
	 * input model.
	 * @param use_stdin Set to <tt>true</tt> to pass the model to NuSMV
	 * through the standard input. Set to <tt>false</tt> to write the model
	 * to a temporary file instead. Currently, NuSMV seems to be unable to
	 * read a model from stdin despite what its documentation says, so it is
	 * advisable to call this method using <tt>false</tt>.
	 * @param model The model to send to NuSMV
	 * @param source_filename The source filename to provide to NuSMV
	 * @return An instance of the command runner, ready to be executed
	 */
	protected CommandRunner getRunner(boolean use_stdin, String model, String source_filename)
	{
		if (use_stdin)
		{
			return new CommandRunner(new String[] {NUSMV_PATH, "-source", source_filename}, model);
		}
		else
		{
			String model_filename = TMP_DIR + FILE_SEPARATOR + "model.smv";
			FileHelper.writeFromString(new File(model_filename), model);
			return new CommandRunner(new String[] {NUSMV_PATH, "-source", source_filename, model_filename});
		}
	}
	
	/**
	 * Gets the model provider associated to this experiment.
	 * @return The model provider
	 */
	public ModelProvider getModelProvider()
	{
		return m_modelProvider;
	}
	
	/**
	 * Gets the property provider associated to this experiment.
	 * @return The property provider
	 */
	public PropertyProvider getPropertyProvider()
	{
		return m_propertyProvider;
	}

	@Override
	public boolean prerequisitesFulfilled()
	{
		return FileHelper.fileExists(getSourceCheckFilename()) && 
				FileHelper.fileExists(getSourceStatsFilename());
	}

	@Override
	public void fulfillPrerequisites()
	{
		writeSourceFiles();
	}

	@Override
	public void cleanPrerequisites()
	{
		FileHelper.deleteFile(getSourceCheckFilename());
	}

	/**
	 * Gets the name of the "source" file containing the batch of commands that
	 * NuSMV should run on the input model for the checking step.
	 * @return The absolute path of the source file
	 */
	/*@ non_null @*/ protected static String getSourceCheckFilename()
	{
		return TMP_DIR + FILE_SEPARATOR + "check.smv";
	}
	
	/**
	 * Gets the name of the "source" file containing the batch of commands that
	 * NuSMV should run on the input model for the stats gathering step.
	 * @return The absolute path of the source file
	 */
	/*@ non_null @*/ protected static String getSourceStatsFilename()
	{
		return TMP_DIR + FILE_SEPARATOR + "stats.smv";
	}

	/**
	 * Writes the "source" files containing the batch of commands that NuSMV
	 * should run on the input model. These files will be the same for all
	 * models sent to the model checker.
	 */
	protected void writeSourceFiles()
	{
		FileHelper.writeFromString(new File(getSourceCheckFilename()), "go; check_property; quit;");
		FileHelper.writeFromString(new File(getSourceStatsFilename()), "go; print_bdd_stats; print_reachable_states; quit;");
	}
}
