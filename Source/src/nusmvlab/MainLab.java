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
import ca.uqac.lif.labpal.server.WebCallback;
import ca.uqac.lif.labpal.table.ExperimentTable;
import ca.uqac.lif.mtnp.plot.TwoDimensionalPlot.Axis;
import ca.uqac.lif.mtnp.plot.gnuplot.Scatterplot;
import ca.uqac.lif.mtnp.table.ExpandAsColumns;
import ca.uqac.lif.mtnp.table.TransformedTable;

import static nusmvlab.ModelProvider.DOMAIN_SIZE;
import static nusmvlab.ModelProvider.QUERY;
import static nusmvlab.ModelProvider.QUEUE_SIZE;
import static nusmvlab.NuSMVExperiment.TIME;
import static nusmvlab.NuSMVModelLibrary.Q_DUMMY;
import static nusmvlab.NuSMVModelLibrary.Q_PASSTHROUGH;
import static nusmvlab.NuSMVModelLibrary.Q_SUM_3;
import static nusmvlab.PropertyProvider.PROPERTY;
import static nusmvlab.StreamPropertyLibrary.P_X_STAYS_NULL;

import java.util.List;

import static nusmvlab.StreamPropertyLibrary.P_NO_FULL_QUEUES;

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
		NuSMVModelLibrary model_library = new NuSMVModelLibrary();
		StreamPropertyLibrary prop_library = new StreamPropertyLibrary(model_library);
		NuSMVExperimentFactory factory = new NuSMVExperimentFactory(this, model_library, prop_library);

		// Big region
		Region r = new Region();
		r.addRange(DOMAIN_SIZE, 1, 10, 2);
		r.addRange(QUEUE_SIZE, 1, 10, 2);
		r.add(QUERY, Q_PASSTHROUGH, Q_SUM_3);
		r.add(PROPERTY, P_NO_FULL_QUEUES);

		// Running time by queue size
		for (Region t_r : r.all(DOMAIN_SIZE))
		{
			ExperimentTable et = new ExperimentTable(QUERY, QUEUE_SIZE, TIME);
			et.setTitle("Running time by queue size (domain = " + t_r.getInt(DOMAIN_SIZE) + ")");
			et.setShowInList(false);
			add(et);
			for (Region t_q : t_r.all(QUERY, PROPERTY, QUEUE_SIZE))
			{
				NuSMVExperiment e = factory.get(t_q);
				if (e == null)
				{
					continue;
				}
				et.add(e);
			}
			TransformedTable tt = new TransformedTable(new ExpandAsColumns(QUERY, TIME), et);
			tt.setTitle(et.getTitle());
			tt.setNickname("tTimeQueue");
			add(tt);
			Scatterplot plot = new Scatterplot(tt);
			plot.setTitle(tt.getTitle());
			plot.setCaption(Axis.X, "Queue size").setCaption(Axis.Y, "Time (ms)");
			plot.setNickname("p" + tt.getNickname());
			add(plot);
		}

		// Running time by domain size
		for (Region t_r : r.all(QUEUE_SIZE))
		{
			ExperimentTable et = new ExperimentTable(QUERY, DOMAIN_SIZE, TIME);
			et.setTitle("Running time by domain size (queue = " + t_r.getInt(QUEUE_SIZE) + ")");
			et.setShowInList(false);
			add(et);
			for (Region t_q : t_r.all(QUERY, PROPERTY, DOMAIN_SIZE))
			{
				NuSMVExperiment e = factory.get(t_q);
				if (e == null)
				{
					continue;
				}
				et.add(e);
			}
			TransformedTable tt = new TransformedTable(new ExpandAsColumns(QUERY, TIME), et);
			tt.setTitle(et.getTitle());
			tt.setNickname("tTimeDomain");
			add(tt);
			Scatterplot plot = new Scatterplot(tt);
			plot.setTitle(tt.getTitle());
			plot.setCaption(Axis.X, "Domain size").setCaption(Axis.Y, "Time (ms)");
			plot.setNickname("p" + tt.getNickname());
			add(plot);
		}
	}
	
	@Override
	public void setupCallbacks(List<WebCallback> callbacks)
	{
		callbacks.add(new ModelPageCallback(this));
	}

	public static void main(String[] args)
	{
		// Nothing else to do here
		MainLab.initialize(args, MainLab.class);
	}
}
