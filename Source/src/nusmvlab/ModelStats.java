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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.uqac.lif.labpal.experiment.Experiment;
import ca.uqac.lif.labpal.Laboratory;
import ca.uqac.lif.labpal.macro.ExperimentMacro;
import ca.uqac.lif.labpal.macro.MacroGroup;

import static nusmvlab.ModelProvider.DOMAIN_SIZE;
import static nusmvlab.ModelProvider.QUEUE_SIZE;

/**
 * Computes statistics about the NuSMV models included in the lab.
 */
public class ModelStats extends MacroGroup
{
	/**
	 * Creates a new instance of the macro.
	 * @param lab The lab to which this macro is associated
	 */
	public ModelStats(Laboratory lab)
	{
		super("Model statistics");
		m_description = "Statistics about the NuSMV models included in the lab";
		add(new MinProcessors(lab, "Minimum processors", "minprocessors", "The minimum number of processors in the chains considered in the lab", lab.getExperiments()));
		add(new MaxProcessors(lab, "Maximum processors", "maxprocessors", "The maximum number of processors in the chains considered in the lab", lab.getExperiments()));
		add(new MaxVariables(lab, "Maximum variables", "maxvariables", "The maximum number of NuSMV variables in all chains considered in the lab", lab.getExperiments()));
		add(new MaxModules(lab, "Maximum modules", "maxmodules", "The maximum number of distinct NuSMV modules in all chains considered in the lab", lab.getExperiments()));
		add(new MaxQueueSize(lab, "Maximum queue size", "maxqueuesize", "The maximum size of the queues in all NuSMV models considered in the lab", lab.getExperiments()));
		add(new NumModels(lab, "Number of models", "nummodels", "The number of distinct NuSMV models considered in the lab", lab.getExperiments()));
	}
	
	protected abstract class ModelMacro extends ExperimentMacro
	{
		protected int m_value;
		
		boolean m_done;
		
		public ModelMacro(Laboratory lab, String name, String nickname, String description, List<Experiment> experiments)
		{
			super(lab, name, nickname);
			add(experiments);
			m_description = description;
			m_value = 0;
			m_done = false;
		}
		
		@Override
		public Object getValue(Set<Experiment> experiments)
		{
			if (m_done)
			{
				return m_value;
			}
			Set<ModelId> ids = new HashSet<ModelId>();
			for (Experiment e : experiments)
			{
				if (!(e instanceof NuSMVExperiment))
				{
					continue;
				}
				NuSMVExperiment ne = (NuSMVExperiment) e;
				ModelProvider mp = ne.getModelProvider();
				ids.add(new ModelId(ne));
				if (mp instanceof BeepBeepModelProvider)
				{
					BeepBeepModelProvider bmp = (BeepBeepModelProvider) mp;
					aggregate(ne, bmp);
				}
			}
			m_done = true;
			return m_value;
		}
		
		protected abstract void aggregate(NuSMVExperiment ne, BeepBeepModelProvider bmp);
		
	}
	
	protected class MaxVariables extends ModelMacro
	{
		public MaxVariables(Laboratory lab, String name, String nickname, String description, List<Experiment> experiments)
		{
			super(lab, name, nickname, description, experiments);
		}

		@Override
		protected void aggregate(NuSMVExperiment ne, BeepBeepModelProvider bmp)
		{
			m_value = Math.max(m_value, bmp.countVariables());
		}
	}
	
	protected class MaxProcessors extends ModelMacro
	{
		public MaxProcessors(Laboratory lab, String name, String nickname, String description, List<Experiment> experiments)
		{
			super(lab, name, nickname, description, experiments);
		}

		@Override
		protected void aggregate(NuSMVExperiment ne, BeepBeepModelProvider bmp)
		{
			m_value = Math.max(m_value, bmp.getNumProcessors());
		}
	}
	
	protected class MinProcessors extends ModelMacro
	{
		public MinProcessors(Laboratory lab, String name, String nickname, String description, List<Experiment> experiments)
		{
			super(lab, name, nickname, description, experiments);
			m_value = 10000;
		}

		@Override
		protected void aggregate(NuSMVExperiment ne, BeepBeepModelProvider bmp)
		{
			m_value = Math.min(m_value, bmp.getNumProcessors());
		}
	}
	
	protected class MaxQueueSize extends ModelMacro
	{
		public MaxQueueSize(Laboratory lab, String name, String nickname, String description, List<Experiment> experiments)
		{
			super(lab, name, nickname, description, experiments);
		}

		@Override
		protected void aggregate(NuSMVExperiment ne, BeepBeepModelProvider bmp)
		{
			m_value = Math.max(m_value, ne.readInt(QUEUE_SIZE));
		}
	}
	
	protected class MaxDomainSize extends ModelMacro
	{
		public MaxDomainSize(Laboratory lab, String name, String nickname, String description, List<Experiment> experiments)
		{
			super(lab, name, nickname, description, experiments);
		}

		@Override
		protected void aggregate(NuSMVExperiment ne, BeepBeepModelProvider bmp)
		{
			m_value = Math.max(m_value, ne.readInt(DOMAIN_SIZE));
		}
	}
	
	protected class MaxModules extends ModelMacro
	{
		public MaxModules(Laboratory lab, String name, String nickname, String description, List<Experiment> experiments)
		{
			super(lab, name, nickname, description, experiments);
		}

		@Override
		protected void aggregate(NuSMVExperiment ne, BeepBeepModelProvider bmp)
		{
			m_value = Math.max(m_value, bmp.getModules().size());
		}
	}
	
	protected class NumModels extends ModelMacro
	{
		public NumModels(Laboratory lab, String name, String nickname, String description, List<Experiment> experiments)
		{
			super(lab, name, nickname, description, experiments);
		}

		@Override
		protected void aggregate(NuSMVExperiment ne, BeepBeepModelProvider bmp)
		{
			m_value++;
		}
	}
}
