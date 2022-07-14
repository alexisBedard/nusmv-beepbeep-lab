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

import ca.uqac.lif.labpal.CliParser;
import ca.uqac.lif.labpal.CliParser.Argument;
import ca.uqac.lif.labpal.CliParser.ArgumentMap;
import ca.uqac.lif.labpal.Group;
import ca.uqac.lif.labpal.Laboratory;
import ca.uqac.lif.labpal.LatexNamer;
import ca.uqac.lif.labpal.Region;
import ca.uqac.lif.labpal.server.WebCallback;
import ca.uqac.lif.labpal.table.ExperimentTable;
import ca.uqac.lif.mtnp.plot.TwoDimensionalPlot.Axis;
import ca.uqac.lif.mtnp.plot.gnuplot.ClusteredHistogram;
import ca.uqac.lif.mtnp.plot.gnuplot.Scatterplot;
import ca.uqac.lif.mtnp.table.ExpandAsColumns;
import ca.uqac.lif.mtnp.table.TransformedTable;
import nusmvlab.StreamPropertyLibrary.BoundedLiveness;
import nusmvlab.StreamPropertyLibrary.Liveness;
import nusmvlab.StreamPropertyLibrary.NoFullQueues;
import nusmvlab.StreamPropertyLibrary.OutputAlwaysTrue;
import nusmvlab.StreamPropertyLibrary.OutputsAlwaysEqual;

import static nusmvlab.BeepBeepModelProvider.K;
import static nusmvlab.ModelProvider.DOMAIN_SIZE;
import static nusmvlab.ModelProvider.QUERY;
import static nusmvlab.ModelProvider.QUEUE_SIZE;
import static nusmvlab.NuSMVExperiment.MEMORY;
import static nusmvlab.NuSMVExperiment.TIME;
import static nusmvlab.NuSMVModelLibrary.Q_COMPARE_WINDOW_SUM_2;
import static nusmvlab.NuSMVModelLibrary.Q_COMPARE_WINDOW_SUM_3;
import static nusmvlab.NuSMVModelLibrary.Q_COMPARE_PASSTHROUGH_DELAY;
import static nusmvlab.NuSMVModelLibrary.Q_OUTPUT_IF_SMALLER_K;
import static nusmvlab.NuSMVModelLibrary.Q_PASSTHROUGH;
import static nusmvlab.NuSMVModelLibrary.Q_PRODUCT_1_K;
import static nusmvlab.NuSMVModelLibrary.Q_PRODUCT_WINDOW_K;
import static nusmvlab.NuSMVModelLibrary.Q_SUM_OF_DOUBLES;
import static nusmvlab.NuSMVModelLibrary.Q_SUM_OF_ODDS;
import static nusmvlab.NuSMVModelLibrary.Q_WIN_SUM_OF_1;
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
		setDoi("10.5281/zenodo.5386462");

		/* Set to true to include experiments performing equivalence and step-wise
		   equivalence checking. */
		boolean include_equivalence = false;

		// Read command line arguments
		{
			ArgumentMap args = getCliArguments();
			if (args.hasOption("with-stats"))
			{
				m_factory.addStats();
			}
			if (args.hasOption("use-nusmv"))
			{
				NuSMVExperiment.NUSMV_PATH = "NuSMV";
			}
		}

		// Impact of queue size and domain size on all processor chains
		{
			Group g_q = new Group("Impact of queue size");
			add(g_q);
			Group g_d = new Group("Impact of domain size");
			add(g_d);
			Region r = new Region();
			r.add(QUERY, Q_PASSTHROUGH, Q_PRODUCT_WINDOW_K, Q_SUM_OF_DOUBLES, Q_SUM_OF_ODDS, Q_PRODUCT_1_K, Q_WIN_SUM_OF_1, Q_OUTPUT_IF_SMALLER_K);
			r.add(PROPERTY, NoFullQueues.NAME, Liveness.NAME);
			r.addRange(DOMAIN_SIZE, 2, 5, 1);
			r.addRange(QUEUE_SIZE, 1, 4, 1);
			for (Region q_r : r.all(QUERY))
			{
				setupQueueDomain(q_r, g_q, g_d);
			}
		}

		// Impact of queue size for the "no full queues" property on all queries
		{
			Group g_q = new Group("Impact of queue size for \"no full queues\"");
			add(g_q);
			Region r = new Region();
			r.add(QUERY, Q_PASSTHROUGH, Q_PRODUCT_WINDOW_K, Q_SUM_OF_DOUBLES, Q_SUM_OF_ODDS, Q_PRODUCT_1_K, Q_WIN_SUM_OF_1, Q_OUTPUT_IF_SMALLER_K);
			r.add(PROPERTY, NoFullQueues.NAME);
			r.add(DOMAIN_SIZE, 5);
			r.addRange(QUEUE_SIZE, 1, 4, 1);
			ExperimentTable et_q_all = new ExperimentTable(QUEUE_SIZE, QUERY, TIME);
			add(et_q_all);
			et_q_all.setShowInList(false);
			TransformedTable tt_q_all = new TransformedTable(new ExpandAsColumns(QUERY, TIME), et_q_all);
			tt_q_all.setTitle("Impact of queue size for \"no full queues\"");
			tt_q_all.setNickname("tImpactQueuesNoFullQueues");
			add(tt_q_all);
			Scatterplot plot = new Scatterplot(tt_q_all);
			plot.setTitle(tt_q_all.getTitle());
			plot.setNickname("pImpactQueuesNoFullQueues");
			for (Region q_r : r.all(QUERY, PROPERTY, QUEUE_SIZE, DOMAIN_SIZE))
			{
				NuSMVExperiment e = m_factory.get(q_r);
				if (e == null)
				{
					continue;
				}
				g_q.add(e);
				et_q_all.add(e);
			}
		}
		
		// Impact of domain size for the "no full queues" property on all queries
				{
					Group g_q = new Group("Impact of domain size for \"no full queues\"");
					add(g_q);
					Region r = new Region();
					r.add(QUERY, Q_PASSTHROUGH, Q_PRODUCT_WINDOW_K, Q_SUM_OF_DOUBLES, Q_SUM_OF_ODDS, Q_PRODUCT_1_K, Q_WIN_SUM_OF_1, Q_OUTPUT_IF_SMALLER_K);
					r.add(PROPERTY, NoFullQueues.NAME);
					r.add(QUEUE_SIZE, 2);
					r.addRange(DOMAIN_SIZE, 2, 5, 1);
					ExperimentTable et_q_all = new ExperimentTable(DOMAIN_SIZE, QUERY, TIME);
					add(et_q_all);
					et_q_all.setShowInList(false);
					TransformedTable tt_q_all = new TransformedTable(new ExpandAsColumns(QUERY, TIME), et_q_all);
					tt_q_all.setTitle("Impact of domain size for \"no full queues\"");
					tt_q_all.setNickname("tImpactDomainsNoFullQueues");
					add(tt_q_all);
					Scatterplot plot = new Scatterplot(tt_q_all);
					plot.setTitle(tt_q_all.getTitle());
					plot.setNickname("pImpactDomainsNoFullQueues");
					for (Region q_r : r.all(QUERY, PROPERTY, QUEUE_SIZE, DOMAIN_SIZE))
					{
						NuSMVExperiment e = m_factory.get(q_r);
						if (e == null)
						{
							continue;
						}
						g_q.add(e);
						et_q_all.add(e);
					}
				}

		// Impact of window width on processors that contain a window
		{
			Group g = new Group("Impact of parameter k");
			add(g);
			Region r = new Region();
			r.add(QUERY, Q_PRODUCT_WINDOW_K, Q_PRODUCT_1_K, Q_WIN_SUM_OF_1, Q_OUTPUT_IF_SMALLER_K);
			r.add(PROPERTY, NoFullQueues.NAME, Liveness.NAME);
			r.add(QUEUE_SIZE, 2);
			r.add(DOMAIN_SIZE, 3);
			r.addRange(K, 2, 5);
			for (Region q_r : r.all(QUERY))
			{
				setupK(q_r, g);
			}
		}

		// Comparison of processor chains on all properties, for a fixed queue size and domain size
		{
			Group g = new Group("Impact of query");
			g.setDescription("Comparison of processor chains on all properties, for a fixed queue size and domain size");
			add(g);
			Region r = new Region();
			r.add(QUERY, Q_PASSTHROUGH, Q_PRODUCT_WINDOW_K, Q_SUM_OF_DOUBLES, Q_PRODUCT_1_K, Q_WIN_SUM_OF_1, Q_OUTPUT_IF_SMALLER_K, Q_SUM_OF_ODDS);
			r.add(PROPERTY, NoFullQueues.NAME, Liveness.NAME, BoundedLiveness.NAME);
			r.add(DOMAIN_SIZE, 4);
			r.add(QUEUE_SIZE, 3);
			ExperimentTable et_time = new ExperimentTable(QUERY, PROPERTY, TIME);
			et_time.setShowInList(false);
			add(et_time);
			TransformedTable tt_time = new TransformedTable(new ExpandAsColumns(PROPERTY, TIME), et_time);
			tt_time.setTitle("Running time by processor chain");
			tt_time.setNickname("tPropertyTime");
			add(tt_time);
			ClusteredHistogram ch = new ClusteredHistogram(tt_time);
			ch.setTitle(tt_time.getTitle());
			ch.setNickname("pPropertyTime");
			add(ch);
			for (Region q_r : r.all(QUERY, PROPERTY))
			{
				NuSMVExperiment e = m_factory.get(q_r);
				if (e == null)
				{
					continue;
				}
				et_time.add(e);
				g.add(e);
			}
		}

		// Sequence equivalence experiments
		if (include_equivalence)
		{
			Group g = new Group("Sequence and step-wise equivalence");
			g.setDescription("These experiments compare two processor pipelines and verify that they always produce identical output streams for any input stream (what is called <em>sequence equivalence</em>), or that they produce the same output at every computation step (<em>stepwise equivalence</em>).");
			add(g);
			Region r = new Region();
			r.add(QUERY, Q_PASSTHROUGH, Q_PRODUCT_WINDOW_K, Q_SUM_OF_DOUBLES, Q_PRODUCT_1_K, Q_WIN_SUM_OF_1, Q_OUTPUT_IF_SMALLER_K, Q_SUM_OF_ODDS, Q_COMPARE_WINDOW_SUM_2, Q_COMPARE_WINDOW_SUM_3, Q_COMPARE_PASSTHROUGH_DELAY);
			r.add(PROPERTY, OutputAlwaysTrue.NAME, OutputsAlwaysEqual.NAME);
			r.add(QUEUE_SIZE, 2);
			r.add(DOMAIN_SIZE, 2);
			ExperimentTable et = new ExperimentTable(QUERY, PROPERTY, TIME);
			et.setShowInList(false);
			TransformedTable tt = new TransformedTable(new ExpandAsColumns(PROPERTY, TIME), et);
			tt.setTitle("Running time for sequence equivalence");
			tt.setNickname("tSequenceEquivalenceTime");
			add(et, tt);
			ClusteredHistogram ch = new ClusteredHistogram(tt);
			ch.setTitle(tt.getTitle());
			ch.setNickname("pSequenceEquivalenceTime");
			add(ch);
			for (Region q_r : r.all(QUERY, PROPERTY))
			{
				NuSMVExperiment e = m_factory.get(q_r);
				if (e == null)
				{
					continue;
				}
				et.add(e);
				g.add(e);
			}
		}

		// Stats
		add(new LabStats(this));
		add(new ModelStats(this));
		add(new TimeStats(this));
	}

	/**
	 * For a given processor chain and a given list of properties to evaluate,
	 * prepares a set of tables and plots that compare both verification time
	 * and memory consumption by varying two parameters: queue size and domain
	 * size.
	 * @param r A region that specifies a unique query, a list of properties,
	 * and a range of values for queue size and domain size
	 * @param g_q If not null, the group to which the queue experiments are
	 * to be added
	 * @param g_d If not null, the group to which the domain experiments are
	 * to be added
	 */
	protected void setupQueueDomain(Region r, Group g_q, Group g_d)
	{
		String query = r.getString(QUERY);
		{
			// Varying queue size
			String latex_query = LatexNamer.latexify(query);
			boolean added = false;
			Region d_r = r.set(DOMAIN_SIZE, 4);
			for (Region t_r : d_r.all(DOMAIN_SIZE))
			{
				String latex_params = LatexNamer.latexify("D" + t_r.getInt(DOMAIN_SIZE));
				ExperimentTable et_time = new ExperimentTable(PROPERTY, QUEUE_SIZE, TIME);
				et_time.setTitle("Running time by queue size for " + query + " (domain = " + t_r.getInt(DOMAIN_SIZE) + ")");
				et_time.setShowInList(false);
				ExperimentTable et_mem = new ExperimentTable(PROPERTY, QUEUE_SIZE, MEMORY);
				et_mem.setTitle("Memory consumption by queue size for " + query + " (domain = " + t_r.getInt(DOMAIN_SIZE) + ")");
				et_mem.setShowInList(false);
				for (Region t_q : t_r.all(QUERY, PROPERTY, QUEUE_SIZE))
				{
					NuSMVExperiment e = m_factory.get(t_q);
					if (e == null)
					{
						continue;
					}
					added = true;
					et_time.add(e);
					et_mem.add(e);
					if (g_q != null)
					{
						g_q.add(e);
					}
				}
				TransformedTable tt_time = new TransformedTable(new ExpandAsColumns(PROPERTY, TIME), et_time);
				tt_time.setTitle(et_time.getTitle());
				tt_time.setNickname("tTimeQueue" + latex_query + latex_params);
				Scatterplot plot_time = new Scatterplot(tt_time);
				plot_time.setTitle(tt_time.getTitle());
				plot_time.setCaption(Axis.X, "Queue size").setCaption(Axis.Y, "Time (ms)");
				plot_time.setNickname("p" + tt_time.getNickname());
				TransformedTable tt_mem = new TransformedTable(new ExpandAsColumns(PROPERTY, MEMORY), et_mem);
				tt_mem.setTitle(et_mem.getTitle());
				tt_mem.setNickname("tmemQueue" + latex_query + latex_params);
				Scatterplot plot_mem = new Scatterplot(tt_mem);
				plot_mem.setTitle(tt_mem.getTitle());
				plot_mem.setCaption(Axis.X, "Queue size").setCaption(Axis.Y, "Memory (B)");
				plot_mem.setNickname("p" + tt_mem.getNickname());				
				if (added)
				{
					add(et_time, tt_time, et_mem, tt_mem);
					add(plot_time, plot_mem);
				}
			}
		}
		{
			// Varying domain size
			String latex_query = LatexNamer.latexify(query);
			Region q_r = r.set(QUEUE_SIZE, 3);
			for (Region t_r : q_r.all(QUEUE_SIZE))
			{
				boolean added = false;
				String latex_params = LatexNamer.latexify("Q" + t_r.getInt(QUEUE_SIZE));
				ExperimentTable et_time = new ExperimentTable(PROPERTY, DOMAIN_SIZE, TIME);
				et_time.setTitle("Running time by domain size for " + query + " (queues = " + t_r.getInt(QUEUE_SIZE) + ")");
				et_time.setShowInList(false);
				ExperimentTable et_mem = new ExperimentTable(PROPERTY, DOMAIN_SIZE, MEMORY);
				et_mem.setTitle("Memory consumption by domain size for " + query + " (queues = " + t_r.getInt(QUEUE_SIZE) + ")");
				et_mem.setShowInList(false);
				for (Region t_q : t_r.all(QUERY, PROPERTY, DOMAIN_SIZE))
				{
					NuSMVExperiment e = m_factory.get(t_q);
					if (e == null)
					{
						continue;
					}
					added = true;
					et_time.add(e);
					et_mem.add(e);
					if (g_d != null)
					{
						g_d.add(e);
					}
				}
				TransformedTable tt_time = new TransformedTable(new ExpandAsColumns(PROPERTY, TIME), et_time);
				tt_time.setTitle(et_time.getTitle());
				tt_time.setNickname("tTimeQueue" + latex_query + latex_params);
				Scatterplot plot_time = new Scatterplot(tt_time);
				plot_time.setTitle(tt_time.getTitle());
				plot_time.setCaption(Axis.X, "Domain size").setCaption(Axis.Y, "Time (ms)");
				plot_time.setNickname("p" + tt_time.getNickname());
				TransformedTable tt_mem = new TransformedTable(new ExpandAsColumns(PROPERTY, MEMORY), et_mem);
				tt_mem.setTitle(et_mem.getTitle());
				tt_mem.setNickname("tmemQueue" + latex_query + latex_params);
				Scatterplot plot_mem = new Scatterplot(tt_mem);
				plot_mem.setTitle(tt_mem.getTitle());
				plot_mem.setCaption(Axis.X, "Domain size").setCaption(Axis.Y, "Memory (B)");
				plot_mem.setNickname("p" + tt_mem.getNickname());
				if (added)
				{
					add(et_time, tt_time, et_mem, tt_mem);
					add(plot_time, plot_mem);
				}
			}
		}
	}

	/**
	 * For a given processor chain and a given list of properties to evaluate,
	 * prepares a set of tables and plots that compare both verification time
	 * and memory consumption by varying the parameter K.
	 * @param r A region that specifies a unique query, a list of properties,
	 * a <em>single</em> value for queue size and domain size, and a range of
	 * values for K.
	 * @param g If not null, the group to which the experiments are to be added
	 */
	protected void setupK(Region r, Group g)
	{
		String query = r.getString(QUERY);
		{
			// Varying K
			boolean added = false;
			String latex_query = LatexNamer.latexify(query);
			String latex_params = LatexNamer.latexify("D" + r.getInt(DOMAIN_SIZE) + "Q" + r.getInt(QUEUE_SIZE));
			ExperimentTable et_time = new ExperimentTable(PROPERTY, QUEUE_SIZE, TIME);
			et_time.setTitle("Running time by value of k for " + query + " (domain = " + r.getInt(DOMAIN_SIZE) + ", queues = " + r.getInt(QUEUE_SIZE) + ")");
			et_time.setShowInList(false);
			ExperimentTable et_mem = new ExperimentTable(PROPERTY, QUEUE_SIZE, MEMORY);
			et_mem.setTitle("Memory consumption by value of k for " + query + " (domain = " + r.getInt(DOMAIN_SIZE) + ", queues = " + r.getInt(QUEUE_SIZE) + ")");
			et_mem.setShowInList(false);
			for (Region t_q : r.all(QUERY, PROPERTY, K))
			{
				NuSMVExperiment e = m_factory.get(t_q);
				if (e == null)
				{
					continue;
				}
				added = true;
				et_time.add(e);
				et_mem.add(e);
				if (g != null)
				{
					g.add(e);
				}
			}
			TransformedTable tt_time = new TransformedTable(new ExpandAsColumns(PROPERTY, TIME), et_time);
			tt_time.setTitle(et_time.getTitle());
			tt_time.setNickname("tTimeK" + latex_query + latex_params);
			Scatterplot plot_time = new Scatterplot(tt_time);
			plot_time.setTitle(tt_time.getTitle());
			plot_time.setCaption(Axis.X, "K").setCaption(Axis.Y, "Time (ms)");
			plot_time.setNickname("p" + tt_time.getNickname());
			TransformedTable tt_mem = new TransformedTable(new ExpandAsColumns(PROPERTY, MEMORY), et_mem);
			tt_mem.setTitle(et_mem.getTitle());
			tt_mem.setNickname("tmemK" + latex_query + latex_params);
			Scatterplot plot_mem = new Scatterplot(tt_mem);
			plot_mem.setTitle(tt_mem.getTitle());
			plot_mem.setCaption(Axis.X, "K").setCaption(Axis.Y, "Memory (B)");
			plot_mem.setNickname("p" + tt_mem.getNickname());
			if (added)
			{
				add(et_time, tt_time, et_mem, tt_mem);
				add(plot_time, plot_mem);
			}
		}
	}

	@Override
	public void setupCallbacks(List<WebCallback> callbacks)
	{
		callbacks.add(new ModelPageCallback(this));
		callbacks.add(new ModelDownloadCallback(this));
		callbacks.add(new InnerFileCallback(this));
		callbacks.add(new AllQueriesCallback(this));
	}

	@Override
	public void setupCli(CliParser parser)
	{
		parser.addArgument(new Argument().withLongName("with-stats").withDescription("Gather stats about state space size (takes much longer)"));
		parser.addArgument(new Argument().withLongName("use-nusmv").withDescription("Call NuSMV instead of nuXmv"));
	}

	public static void main(String[] args)
	{
		// Nothing else to do here
		MainLab.initialize(args, MainLab.class);
	}
}
