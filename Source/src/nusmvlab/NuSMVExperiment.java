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
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
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
		}
		catch (UnsupportedEncodingException e)
		{
			throw new ExperimentException(e);
		}
		m_modelProvider.printToFile(ps);
		if (m_propertyProvider.getLogic() == Logic.CTL)
		{
			ps.println("CTLSPEC");
		}
		else
		{
			ps.println("LTLSPEC");
		}
		m_propertyProvider.printToFile(ps);
		ps.close();
		String model = baos.toString();
		long start_time = System.currentTimeMillis();
		runNuSMV(model);
		long end_time = System.currentTimeMillis();
		write(TIME, end_time - start_time);
	}
	
	protected void runNuSMV(String model) throws ExperimentException
	{
		CommandRunner runner = getRunner(false, model);
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
		write(MEMORY, readIntFromOutput(output, s_memoryPattern));
		write(TOTAL_NODES, readIntFromOutput(output, s_totalNodesPattern));
		write(LIVE_NODES, readIntFromOutput(output, s_liveNodesPattern));
	}
	
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
	 * Gets an instance of {@link CommandRunner} that calls NuSMV on the
	 * input model.
	 * @param use_stdin Set to <tt>true</tt> to pass the model to NuSMV
	 * through the standard input. Set to <tt>false</tt> to write the model
	 * to a temporary file instead. Currently, NuSMV seems to be unable to
	 * read a model from stdin despite what its documentation says, so it is
	 * advisable to call this method using <tt>false</tt>.
	 * @param model The model to send to NuSMV
	 * @return An instance of the command runner, ready to be executed
	 */
	protected CommandRunner getRunner(boolean use_stdin, String model)
	{
		if (use_stdin)
		{
			return new CommandRunner(new String[] {NUSMV_PATH, "-source", getSourceFilename()}, model);
		}
		else
		{
			String model_filename = TMP_DIR + FILE_SEPARATOR + "model.smv";
			FileHelper.writeFromString(new File(model_filename), model);
			return new CommandRunner(new String[] {NUSMV_PATH, "-source", getSourceFilename(), model_filename});
		}
	}
	
	@Override
	public boolean prerequisitesFulfilled()
	{
		return FileHelper.fileExists(getSourceFilename());
	}
	
	@Override
	public void fulfillPrerequisites()
	{
		writeSourceFile();
	}
	
	@Override
	public void cleanPrerequisites()
	{
		FileHelper.deleteFile(getSourceFilename());
	}
	
	/**
	 * Gets the name of the "source" file containing the batch of commands that
	 * NuSMV should run on the input model.
	 * @return The absolute path of the source file
	 */
	/*@ non_null @*/ protected static String getSourceFilename()
	{
		return TMP_DIR + FILE_SEPARATOR + "commands.smv";
	}
	
	/**
	 * Writes a "source" file containing the batch of commands that NuSMV
	 * should run on the input model. This file will be the same for all
	 * models sent to the model checker.
	 */
	protected void writeSourceFile()
	{
		FileHelper.writeFromString(new File(getSourceFilename()), "go; check_property; print_bdd_stats; quit;");
	}
}
