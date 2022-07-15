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

import ca.uqac.lif.labpal.experiment.Experiment;
import ca.uqac.lif.labpal.Laboratory;
import ca.uqac.lif.labpal.Stateful;
import ca.uqac.lif.labpal.macro.Macro;

import static nusmvlab.ModelProvider.QUERY;
import static nusmvlab.PropertyProvider.PROPERTY;

/**
 * Computes statistics about NuSMV's running time for the various experiments.
 */
public class TimeStats extends Macro
{
	/**
	 * Creates a new instance of the macro.
	 * @param lab The lab to which this macro is associated
	 */
	public TimeStats(Laboratory lab)
	{
		super(lab);
		add("maxtime", "The maximum time taken by NuSMV to verify a model");
		add("maxquery", "The pipeline for which the maximum execution time has been observed");
		add("maxproperty", "The property for which the maximum execution time has been observed");
	}

	@Override
	public void computeValues(Map<String, Object> paramMap)
	{
		int max_time = 0;
		String property = "", pipeline = "";
		for (Experiment e : m_lab.getExperiments())
		{
			if (!(e instanceof NuSMVExperiment) || e.getStatus() != Status.DONE)
			{
				continue;
			}
			NuSMVExperiment ne = (NuSMVExperiment) e;
			int time = ne.readInt(NuSMVExperiment.TIME);
			if (time > max_time)
			{
				max_time = time;
				property = ne.readString(PROPERTY);
				pipeline = ne.readString(QUERY);
			}
		}
		paramMap.put("maxtime", max_time);
		paramMap.put("maxquery", pipeline);
		paramMap.put("maxproperty", property);
	}

	@Override
	public Status getStatus()
	{
		return Stateful.getLowestStatus(m_lab.getExperiments());
	}

	@Override
	public void reset()
	{
		for (Experiment e : m_lab.getExperiments())
		{
			e.reset();
		}
	}

	@Override
	public float getProgression()
	{
		float t = 0, n = 0;
		for (Experiment e : m_lab.getExperiments())
		{
			t += e.getProgression();
			n++;
		}
		if (n == 0)
		{
			return 1;
		}
		return t / n;
	}

	@Override
	public String getNickname()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
