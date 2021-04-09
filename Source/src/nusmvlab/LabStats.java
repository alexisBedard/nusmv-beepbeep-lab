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

import java.util.Map;

import ca.uqac.lif.json.JsonElement;
import ca.uqac.lif.json.JsonMap;
import ca.uqac.lif.json.JsonNumber;
import ca.uqac.lif.json.JsonParser;
import ca.uqac.lif.json.JsonParser.JsonParseException;
import ca.uqac.lif.json.JsonString;
import ca.uqac.lif.labpal.FileHelper;
import ca.uqac.lif.labpal.Laboratory;
import ca.uqac.lif.labpal.macro.MacroMap;

/**
 * Computes various static parameters about the environment in which the
 * lab is executed.
 */
public class LabStats extends MacroMap
{
	protected transient JsonMap m_fileContents;

	/**
	 * Instantiates the macro and defines its named data points
	 * @param lab The lab from which to fetch the values
	 */
	public LabStats(Laboratory lab)
	{
		super(lab);
		m_fileContents = null;
		String host = m_lab.getHostName();
		JsonParser parser = new JsonParser();
		try 
		{
			JsonMap je = (JsonMap) parser.parse(FileHelper.internalFileToString(MainLab.class, "machine-specs.json"));
			if (je.containsKey(host))
			{
				m_fileContents = (JsonMap) je.get(host);
			}
		}
		catch (JsonParseException e)
		{
			// Do nothing
		}
		add("machinestring", "Basic info about the machine running the lab");
		add("machineram", "Total memory in the machine running the lab");
		add("jvmram", "RAM available to the JVM");
		add("numexperiments", "The number of experiments in the lab");
		add("numdatapoints", "The number of data points in the lab");
	}

	@Override
	public void computeValues(Map<String,JsonElement> map)
	{
		map.put("machinestring", new JsonString(getMachineString()));
		map.put("machineram", new JsonString(getMachineRam()));
		map.put("jvmram", new JsonString(getMemory()));
		map.put("numexperiments", new JsonNumber(m_lab.getExperimentIds().size()));
		map.put("numdatapoints", new JsonNumber(m_lab.countDataPoints()));
	}

	protected String getMemory()
	{
		long mem = Runtime.getRuntime().maxMemory();
		long mem_in_mb = mem / (1024 * 1024);
		return Long.toString(mem_in_mb);
	}

	protected String getMachineString()
	{
		if (m_fileContents == null)
		{
			return "";
		}
		return ((JsonString) m_fileContents.get("CPU")).stringValue() + " running " 
		+ ((JsonString) m_fileContents.get("OS")).stringValue();
	}

	protected String getMachineRam()
	{
		if (m_fileContents == null)
		{
			return "";
		}
		return ((JsonString) m_fileContents.get("RAM")).stringValue(); 
	}
}
