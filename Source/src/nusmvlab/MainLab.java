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

import ca.uqac.lif.json.JsonElement;
import ca.uqac.lif.json.JsonString;
import ca.uqac.lif.labpal.Laboratory;
import ca.uqac.lif.labpal.LatexNamer;
import ca.uqac.lif.labpal.Region;
import ca.uqac.lif.labpal.server.WebCallback;
import ca.uqac.lif.labpal.table.ExperimentTable;
import ca.uqac.lif.mtnp.plot.TwoDimensionalPlot.Axis;
import ca.uqac.lif.mtnp.plot.gnuplot.Scatterplot;
import ca.uqac.lif.mtnp.table.ExpandAsColumns;
import ca.uqac.lif.mtnp.table.TransformedTable;
import nusmvlab.StreamPropertyLibrary.Liveness;
import nusmvlab.StreamPropertyLibrary.NoFullQueues;

import static nusmvlab.ModelProvider.DOMAIN_SIZE;
import static nusmvlab.ModelProvider.QUERY;
import static nusmvlab.ModelProvider.QUEUE_SIZE;
import static nusmvlab.NuSMVExperiment.MEMORY;
import static nusmvlab.NuSMVExperiment.TIME;
import static nusmvlab.NuSMVModelLibrary.Q_DUMMY;
import static nusmvlab.NuSMVModelLibrary.Q_PASSTHROUGH;
import static nusmvlab.NuSMVModelLibrary.Q_SUM_3;
import static nusmvlab.NuSMVModelLibrary.Q_SUM_OF_DOUBLES;
import static nusmvlab.PropertyProvider.PROPERTY;

import java.util.List;

/**
 * The lab that evaluates NuSMV translations of BeepBeep processor chains.
 */
public class MainLab extends Laboratory
{
	//Experiment factory
	protected final transient NuSMVModelLibrary m_modelLibrary = new NuSMVModelLibrary();
	protected final transient StreamPropertyLibrary m_propLibrary = new StreamPropertyLibrary(m_modelLibrary);
	protected final transient NuSMVExperimentFactory m_factory = new NuSMVExperimentFactory(this, m_modelLibrary, m_propLibrary);

	@Override
	public void setup()
	{
		// Lab metadata
		setTitle("A benchmark for NuSMV extensions to BeepBeep 3");
		setAuthor("Alexis Bédard and Sylvain Hallé");

		// Big region
		{
			Region r = new Region();
			r.add(QUERY, Q_PASSTHROUGH); //, Q_SUM_3, Q_SUM_OF_DOUBLES);		
			for (Region q_r : r.all(QUERY))
			{
				setupQueueDomain(q_r.getString(QUERY), NoFullQueues.NAME, Liveness.NAME);
			}
		}
		
		// Stats
		add(new LabStats(this));
		add(new ModelStats(this));
	}

	/**
	 * For a given processor chain and a given list of properties to evaluate,
	 * prepares a set of tables and plots that compare both verification time
	 * and memory consumption by varying two parameters: queue size and domain
	 * size.
	 * @param query The name of the processor chain to consider
	 * @param properties A list of properties to evaluate
	 */
	protected void setupQueueDomain(String query, String ... properties)
	{
		Region r = new Region();
		r.add(QUERY, query);
		JsonElement[] props = new JsonElement[properties.length];
		for (int i = 0; i < properties.length; i++)
		{
			props[i] = new JsonString(properties[i]);
		}
		r.add(PROPERTY, props);
		r.addRange(DOMAIN_SIZE, 2, 10, 2);
		r.addRange(QUEUE_SIZE, 1, 10, 2);
		{
			// Varying queue size
			String latex_query = LatexNamer.latexify(query);
			for (Region t_r : r.all(DOMAIN_SIZE))
			{
				ExperimentTable et_time = new ExperimentTable(PROPERTY, QUEUE_SIZE, TIME);
				et_time.setTitle("Running time by queue size for " + query + " (domain = " + t_r.getInt(DOMAIN_SIZE) + ")");
				et_time.setShowInList(false);
				add(et_time);
				ExperimentTable et_mem = new ExperimentTable(PROPERTY, QUEUE_SIZE, MEMORY);
				et_mem.setTitle("Memory consumption by queue size for " + query + " (domain = " + t_r.getInt(DOMAIN_SIZE) + ")");
				et_mem.setShowInList(false);
				add(et_time);
				for (Region t_q : t_r.all(QUERY, PROPERTY, QUEUE_SIZE))
				{
					NuSMVExperiment e = m_factory.get(t_q);
					if (e == null)
					{
						continue;
					}
					et_time.add(e);
					et_mem.add(e);
				}
				TransformedTable tt_time = new TransformedTable(new ExpandAsColumns(PROPERTY, TIME), et_time);
				tt_time.setTitle(et_time.getTitle());
				tt_time.setNickname("tTimeQueue" + latex_query);
				add(tt_time);
				Scatterplot plot_time = new Scatterplot(tt_time);
				plot_time.setTitle(tt_time.getTitle());
				plot_time.setCaption(Axis.X, "Queue size").setCaption(Axis.Y, "Time (ms)");
				plot_time.setNickname("p" + tt_time.getNickname());
				add(plot_time);
				TransformedTable tt_mem = new TransformedTable(new ExpandAsColumns(PROPERTY, MEMORY), et_mem);
				tt_mem.setTitle(et_mem.getTitle());
				tt_mem.setNickname("tmemQueue" + latex_query);
				add(tt_mem);
				Scatterplot plot_mem = new Scatterplot(tt_mem);
				plot_mem.setTitle(tt_mem.getTitle());
				plot_mem.setCaption(Axis.X, "Queue size").setCaption(Axis.Y, "Memory (B)");
				plot_mem.setNickname("p" + tt_mem.getNickname());
				add(plot_mem);
			}
		}
		{
			// Varying domain size
			String latex_query = LatexNamer.latexify(query);
			for (Region t_r : r.all(QUEUE_SIZE))
			{
				ExperimentTable et_time = new ExperimentTable(PROPERTY, DOMAIN_SIZE, TIME);
				et_time.setTitle("Running time by domain size for " + query + " (domain = " + t_r.getInt(DOMAIN_SIZE) + ")");
				et_time.setShowInList(false);
				add(et_time);
				ExperimentTable et_mem = new ExperimentTable(PROPERTY, DOMAIN_SIZE, MEMORY);
				et_mem.setTitle("Memory consumption by domain size for " + query + " (domain = " + t_r.getInt(DOMAIN_SIZE) + ")");
				et_mem.setShowInList(false);
				add(et_time);
				for (Region t_q : t_r.all(QUERY, PROPERTY, DOMAIN_SIZE))
				{
					NuSMVExperiment e = m_factory.get(t_q);
					if (e == null)
					{
						continue;
					}
					et_time.add(e);
					et_mem.add(e);
				}
				TransformedTable tt_time = new TransformedTable(new ExpandAsColumns(PROPERTY, TIME), et_time);
				tt_time.setTitle(et_time.getTitle());
				tt_time.setNickname("tTimeQueue" + latex_query);
				add(tt_time);
				Scatterplot plot_time = new Scatterplot(tt_time);
				plot_time.setTitle(tt_time.getTitle());
				plot_time.setCaption(Axis.X, "Domain size").setCaption(Axis.Y, "Time (ms)");
				plot_time.setNickname("p" + tt_time.getNickname());
				add(plot_time);
				TransformedTable tt_mem = new TransformedTable(new ExpandAsColumns(PROPERTY, MEMORY), et_mem);
				tt_mem.setTitle(et_mem.getTitle());
				tt_mem.setNickname("tmemQueue" + latex_query);
				add(tt_mem);
				Scatterplot plot_mem = new Scatterplot(tt_mem);
				plot_mem.setTitle(tt_mem.getTitle());
				plot_mem.setCaption(Axis.X, "Domain size").setCaption(Axis.Y, "Memory (B)");
				plot_mem.setNickname("p" + tt_mem.getNickname());
				add(plot_mem);
			}
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
