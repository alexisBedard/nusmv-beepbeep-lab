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

import ca.uqac.lif.labpal.Laboratory;
import ca.uqac.lif.labpal.Region;
import ca.uqac.lif.labpal.table.ExperimentTable;
import ca.uqac.lif.mtnp.plot.TwoDimensionalPlot.Axis;
import ca.uqac.lif.mtnp.plot.gnuplot.Scatterplot;
import ca.uqac.lif.mtnp.table.ExpandAsColumns;
import ca.uqac.lif.mtnp.table.TransformedTable;

import static nusmvlab.BeepBeepModelProvider.DOMAIN_SIZE;
import static nusmvlab.BeepBeepModelProvider.QUERY;
import static nusmvlab.BeepBeepModelProvider.QUEUE_SIZE;
import static nusmvlab.NuSMVExperiment.TIME;
import static nusmvlab.NuSMVExperimentFactory.Q_DUMMY;
import static nusmvlab.NuSMVExperimentFactory.Q_PASSTHROUGH;

/**
 * The lab that evaluates NuSMV translations of BeepBeep processor chains.
 */
public class MainLab extends Laboratory
{
	@Override
	public void setup()
	{
		// Lab metadata
		setTitle("A benchmark for NuSMV extensions to BeepBeep 3");
		setAuthor("Alexis Bédard and Sylvain Hallé");
		
		// Experiment factory
		NuSMVExperimentFactory factory = new NuSMVExperimentFactory(this);
		
		// Big region
		Region r = new Region();
		r.addRange(DOMAIN_SIZE, 1, 3);
		r.addRange(QUEUE_SIZE, 1, 3);
		r.add(QUERY, Q_DUMMY, Q_PASSTHROUGH);
		
		// Running time by queue size
		for (Region t_r : r.all(DOMAIN_SIZE))
		{
			ExperimentTable et = new ExperimentTable(QUERY, QUEUE_SIZE, TIME);
			et.setTitle("Running time by queue size (domain = " + t_r.getInt(DOMAIN_SIZE) + ")");
			et.setShowInList(false);
			add(et);
			for (Region t_q : t_r.all(QUERY, QUEUE_SIZE))
			{
				NuSMVExperiment e = factory.get(t_q);
				if (e == null)
				{
					continue;
				}
				et.add(e);
			}
			TransformedTable tt = new TransformedTable(new ExpandAsColumns(QUEUE_SIZE, TIME), et);
			tt.setTitle(et.getTitle());
			tt.setNickname("tTimeQueue");
			add(tt);
			Scatterplot plot = new Scatterplot(tt);
			plot.setTitle(tt.getTitle());
			plot.setCaption(Axis.X, "Queue size").setCaption(Axis.Y, "Time (ms)");
			plot.setNickname("p" + tt.getNickname());
			add(plot);
		}
	}
	
	public static void main(String[] args)
	{
		// Nothing else to do here
		MainLab.initialize(args, MainLab.class);
	}
}
