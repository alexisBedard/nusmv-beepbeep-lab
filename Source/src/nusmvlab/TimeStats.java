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

import java.util.List;
import java.util.Set;

import ca.uqac.lif.labpal.experiment.Experiment;
import ca.uqac.lif.labpal.Laboratory;
import ca.uqac.lif.labpal.macro.ExperimentMacro;
import ca.uqac.lif.labpal.macro.MacroGroup;

import static nusmvlab.ModelProvider.QUERY;
import static nusmvlab.PropertyProvider.PROPERTY;

/**
 * Computes statistics about NuSMV's running time for the various experiments.
 */
public class TimeStats extends MacroGroup
{
	/**
	 * Creates a new instance of the macro.
	 * @param lab The lab to which this macro is associated
	 */
	public TimeStats(Laboratory lab)
	{
		super("Time statistics");
		m_description = "Statistics about verification time";
		add(new MaxTime(lab, "Maximum running time", "maxtime", "The maximum time taken by NuSMV to verify a model", lab.getExperiments()));
		add(new MaxPipeline(lab, "Pipeline with maximum running time", "maxquery", "The pipeline for which the maximum execution time has been observed", lab.getExperiments()));
		add(new MaxProperty(lab, "Property with maximum running time", "maxproperty", "The property for which the maximum execution time has been observed", lab.getExperiments()));
	}
	
	protected class MaxTime extends ExperimentMacro
	{
		public MaxTime(Laboratory lab, String name, String nickname, String description, List<Experiment> experiments)
		{
			super(lab, name, nickname);
			add(experiments);
			m_description = description;
		}
		
		@Override
		public Object getValue(Set<Experiment> experiments)
		{
			int max_time = 0;
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
				}
			}
			return max_time;
		}
	}
	
	protected class MaxPipeline extends ExperimentMacro
	{
		public MaxPipeline(Laboratory lab, String name, String nickname, String description, List<Experiment> experiments)
		{
			super(lab, name, nickname);
			add(experiments);
			m_description = description;
		}
		
		@Override
		public Object getValue(Set<Experiment> experiments)
		{
			int max_time = 0;
			String pipeline = "";
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
					pipeline = ne.readString(QUERY);
				}
			}
			return pipeline;
		}
	}

	protected class MaxProperty extends ExperimentMacro
	{
		public MaxProperty(Laboratory lab, String name, String nickname, String description, List<Experiment> experiments)
		{
			super(lab, name, nickname);
			add(experiments);
			m_description = description;
		}
		
		@Override
		public Object getValue(Set<Experiment> experiments)
		{
			int max_time = 0;
			String property = "";
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
				}
			}
			return property;
		}
	}
}
