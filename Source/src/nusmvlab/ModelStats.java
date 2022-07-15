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
import java.util.Map;
import java.util.Set;

import ca.uqac.lif.labpal.experiment.Experiment;
import ca.uqac.lif.labpal.Laboratory;
import ca.uqac.lif.labpal.Stateful;
import ca.uqac.lif.labpal.macro.Macro;

import static nusmvlab.ModelProvider.DOMAIN_SIZE;
import static nusmvlab.ModelProvider.QUEUE_SIZE;

/**
 * Computes statistics about the NuSMV models included in the lab.
 */
public class ModelStats extends Macro
{
	/**
	 * Creates a new instance of the macro.
	 * @param lab The lab to which this macro is associated
	 */
	public ModelStats(Laboratory lab)
	{
		super(lab);
		add("minprocessors", "The minimum number of processors in the chains considered in the lab");
		add("maxprocessors", "The maximum number of processors in the chains considered in the lab");
		add("maxvariables", "The maximum number of NuSMV variables in all chains considered in the lab");
		add("maxmodules", "The maximum number of distinct NuSMV modules in all chains considered in the lab");
		add("maxqueuesize", "The maximum size of the queues in all NuSMV models considered in the lab");
		add("nummodels", "The number of distinct NuSMV models considered in the lab");
	}

	@Override
	public void computeValues(Map<String,Object> paramMap)
	{
		int min_procs = 1000, max_procs = 0, max_vars = 0, max_modules = 0, max_queue = 0, max_domain = 0;
		Set<ModelId> ids = new HashSet<ModelId>();
		for (Experiment e : m_lab.getExperiments())
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
				max_vars = Math.max(max_vars, bmp.countVariables());
				max_modules = Math.max(max_modules, bmp.getModules().size());
				int num_procs = bmp.getNumProcessors();
				min_procs = Math.min(min_procs, num_procs);
				max_procs = Math.max(max_procs, num_procs);
				max_queue = Math.max(max_queue, ne.readInt(QUEUE_SIZE));
				max_domain = Math.max(max_domain, ne.readInt(DOMAIN_SIZE));
			}
		}
		paramMap.put("minprocessors", min_procs);
		paramMap.put("maxprocessors", max_procs);
		paramMap.put("maxmodules", max_modules);
		paramMap.put("maxvariables", max_vars);
		paramMap.put("maxqueuesize", max_queue);
		paramMap.put("maxdomainsize", max_domain);
		paramMap.put("nummodels", ids.size());
	}

	@Override
	public Status getStatus()
	{
		return Stateful.Status.DONE;
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
		return 1;
	}

	@Override
	public String getNickname()
	{
		return "";
	}
}
