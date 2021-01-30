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

import ca.uqac.lif.labpal.CommandRunner;
import ca.uqac.lif.labpal.Experiment;
import ca.uqac.lif.labpal.ExperimentException;
import ca.uqac.lif.labpal.FileHelper;

/**
 * Experiment that runs NuSMV on a given input model, evaluates a CTL or LTL
 * property, and gathers statistics about the process.
 */
public abstract class NuSMVExperiment extends Experiment
{
	/**
	 * The name of attribute "Time".
	 */
	public static final transient String TIME = "Time";
	
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
	 * An object that provides a NuSMV file to the experiment.
	 */
	protected transient NuSMVModelProvider m_modelProvider;
	
	/**
	 * Creates a new instance of NuSMVExperiment.
	 * @param p  An object that provides a NuSMV file to the experiment
	 */
	public NuSMVExperiment(/*@ non_null @*/ NuSMVModelProvider p)
	{
		super();
		m_modelProvider = p;
		describe(TIME, "The time (in ms) taken to process the NuSMV model");
		m_modelProvider.fillExperiment(this);
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
		m_modelProvider.getModel(ps);
		ps.close();
		String model = baos.toString();
		long start_time = System.currentTimeMillis();
		runNuSMV(model);
		long end_time = System.currentTimeMillis();
		write(TIME, end_time - start_time);
	}
	
	protected void runNuSMV(String model) throws ExperimentException
	{
		CommandRunner runner = new CommandRunner(new String[] {"-source", getSourceFilename(), NUSMV_PATH}, model);
		runner.run();
		String output = new String(runner.getBytes());
		int outcode = runner.getErrorCode();
		if (outcode != 0)
		{
			throw new ExperimentException("NuSMV existed with code " + outcode);
		}
		// TODO: parse NuSMV output and write stats into experiment
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
