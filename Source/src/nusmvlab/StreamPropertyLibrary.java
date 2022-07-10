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

import ca.uqac.lif.nusmv4j.ArrayVariable;
import ca.uqac.lif.labpal.Region;

import static nusmvlab.PropertyProvider.PROPERTY;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Set;

/**
 * Library that produces property providers based on the contents of a
 * region.
 */
public class StreamPropertyLibrary implements Library<PropertyProvider>
{	
	/**
	 * A library that can be used to fetch models. Some properties are expressed
	 * differently depending on the actual NuSMV file on which they are applied.
	 */
	protected transient NuSMVModelLibrary m_models;
	
	/**
	 * Creates a new instance of the library.
	 */
	public StreamPropertyLibrary(NuSMVModelLibrary models)
	{
		super();
		m_models = models;
	}
	
	@Override
	public PropertyProvider get(Region r)
	{
		String name = r.getString(PROPERTY);
		ModelProvider model = m_models.get(r);
		if (name == null || model == null)
		{
			return null;
		}
		if (name.compareTo(XStaysNull.NAME) == 0)
		{
			return new XStaysNull();
		}
		BeepBeepModelProvider b_model = (BeepBeepModelProvider) model;
		if (name.compareTo(Liveness.NAME) == 0)
		{
			return new Liveness(b_model.getInputPipeIds(), b_model.getOutputPipeIds());
		}
		if (name.compareTo(BoundedLiveness.NAME) == 0)
		{
			return new BoundedLiveness(b_model.getInputPipeIds(), b_model.getOutputPipeIds());
		}
		if (name.compareTo(OutputAlwaysEven.NAME) == 0)
		{
			return new OutputAlwaysEven(b_model.getOutputPipeIds());
		}
		if (name.compareTo(OutputAlwaysTrue.NAME) == 0)
		{
			return new OutputAlwaysTrue(b_model.getOutputPipeIds());
		}
		if (name.compareTo(OutputsAlwaysEqual.NAME) == 0)
		{
			return new OutputsAlwaysEqual(b_model.getOutputPipeIds());
		}
		if (name.compareTo(NoFullQueues.NAME) == 0)
		{
			return new NoFullQueues(b_model.getQueueVariables());
		}
		return null;
	}
	
	/**
	 * Stipulates that a state variable x, when it reaches 0, remains at 0
	 * forever.
	 */
	protected static class XStaysNull extends CTLPropertyProvider
	{
		/**
		 * The name of query "x stays null"
		 */
		public static final transient String NAME = "x stays null";
		
		/**
		 * Creates a new instance of the property.
		 */
		public XStaysNull()
		{
			super(NAME);
		}

		@Override
		public void printToFile(PrintStream ps)
		{
			ps.println("AG (x = 0 -> AG (x = 0));");
		}
	}
	
	/**
	 * Stipulates that a processor chain can always output one more event.
	 */
	protected static class Liveness extends LTLPropertyProvider
	{
		/**
		 * The name of query "Output always even"
		 */
		public static final transient String NAME = "Liveness";
		
		/**
		 * The set of IDs corresponding to the outputs of the processor chain.
		 */
		protected final Set<Integer> m_inputPipeIds;
		
		/**
		 * The set of IDs corresponding to the outputs of the processor chain.
		 */
		protected final Set<Integer> m_outputPipeIds;
		
		/**
		 * Creates a new instance of the property.
		 * @param input_pipe_ids The set of IDs corresponding to the inputs of
		 * the processor chain
		 * @param output_pipe_ids The set of IDs corresponding to the outputs of
		 * the processor chain
		 */
		public Liveness(Set<Integer> input_pipe_ids, Set<Integer> output_pipe_ids)
		{
			super(NAME);
			m_inputPipeIds = input_pipe_ids;
			m_outputPipeIds = output_pipe_ids;
		}

		@Override
		public void printToFile(PrintStream ps)
		{
			int i = 0;
			for (int id : m_outputPipeIds)
			{
				if (i > 0)
				{
					ps.print(" & ");
				}
				int j = 0;
				ps.print("G ((");
				for (int in_id : m_inputPipeIds)
				{
					if (j > 0)
					{
						ps.print(" & ");
					}
					ps.print("inb_" + in_id + "[0]");
					j++;
				}
				ps.print(") -> (F ob_" + id + "[0]))");
				i++;
			}
			ps.println(";");
		}
	}
	
	/**
	 * Stipulates that a processor chain can always output one more event.
	 */
	protected static class BoundedLiveness extends LTLPropertyProvider
	{
		/**
		 * The name of query "Bounded liveness"
		 */
		public static final transient String NAME = "Bounded liveness";
		
		/**
		 * The set of IDs corresponding to the outputs of the processor chain.
		 */
		protected final Set<Integer> m_inputPipeIds;
		
		/**
		 * The set of IDs corresponding to the outputs of the processor chain.
		 */
		protected final Set<Integer> m_outputPipeIds;
		
		/**
		 * Creates a new instance of the property.
		 * @param output_pipe_ids The set of IDs corresponding to the outputs of
		 * the processor chain
		 */
		public BoundedLiveness(Set<Integer> input_pipe_ids, Set<Integer> output_pipe_ids)
		{
			super(NAME);
			m_inputPipeIds = input_pipe_ids;
			m_outputPipeIds = output_pipe_ids;
		}

		@Override
		public void printToFile(PrintStream ps)
		{
			int i = 0;
			for (int id : m_outputPipeIds)
			{
				if (i > 0)
				{
					ps.print(" & ");
				}
				int j = 0;
				ps.print("G ((");
				for (int in_id : m_inputPipeIds)
				{
					if (j > 0)
					{
						ps.print(" & ");
					}
					ps.print("inb_" + in_id + "[0]");
					j++;
				}
				ps.print(") -> (ob_" + id + "[0] |  X (ob_" + id + "[0] | X (ob_" + id + "[0]))))");
				i++;
			}
			ps.println(";");
		}
	}
	
	/**
	 * Stipulates that the output of a processor chain is always an even number.
	 */
	protected static class OutputAlwaysEven extends CTLPropertyProvider
	{
		/**
		 * The name of query "Output always even"
		 */
		public static final transient String NAME = "Output always even";
		
		/**
		 * The set of IDs corresponding to the outputs of the processor chain.
		 */
		protected Set<Integer> m_pipeIds;
		
		/**
		 * Creates a new instance of the property.
		 * @param pipe_ids The set of IDs corresponding to the outputs of
		 * the processor chain
		 */
		public OutputAlwaysEven(Set<Integer> pipe_ids)
		{
			super(NAME);
			m_pipeIds = pipe_ids;
		}

		@Override
		public void printToFile(PrintStream ps)
		{
			int i = 0;
			for (int id : m_pipeIds)
			{
				if (i > 0)
				{
					ps.print(" & ");
				}
				ps.print("(AG (ob_" + id + "[0] -> (oc_" + id + "[0] mod 2) = 0)))");
				i++;
			}
			ps.println(";");
		}
	}
	
	/**
	 * Stipulates that all the outputs of a processor chain produce the same
	 * values at the same time.
	 */
	protected static class OutputsAlwaysEqual extends LTLPropertyProvider
	{
		/**
		 * The name of query "Outputs always equal"
		 */
		public static final transient String NAME = "Stepwise equivalence";
		
		/**
		 * The set of IDs corresponding to the outputs of the processor chain.
		 */
		protected Set<Integer> m_pipeIds;
		
		/**
		 * Creates a new instance of the property.
		 * @param pipe_ids The set of IDs corresponding to the outputs of
		 * the processor chain
		 */
		public OutputsAlwaysEqual(Set<Integer> pipe_ids)
		{
			super(NAME);
			m_pipeIds = pipe_ids;
		}

		@Override
		public void printToFile(PrintStream ps)
		{
			if (m_pipeIds.size() < 2)
			{
				// You need at least two outputs to state that they are equal
				ps.print("TRUE;");
				return;
			}
			ps.print("G (");
			int i = 0;
			for (int id1 : m_pipeIds)
			{
				for (int id2 : m_pipeIds)
				{
					if (id2 <= id1)
					{
						continue;
					}
					if (i > 0)
					{
						ps.print(" & ");
					}
					ps.print("(ob_" + id1 + "[0] = ob_" + id2 + "[0] & (ob_" + id1 + "[0] -> (oc_" + id1 + "[0] = oc_" + id2 + "[0])))");
					i++;
				}
				i++;
			}
			ps.println(");");
		}
	}
	
	/**
	 * Stipulates that the output of a processor chain is always the Boolean
	 * value <tt>true</tt>.
	 */
	protected static class OutputAlwaysTrue extends LTLPropertyProvider
	{
		/**
		 * The name of query "Output always true"
		 */
		public static final transient String NAME = "Sequence equivalence";
		
		/**
		 * The set of IDs corresponding to the outputs of the processor chain.
		 */
		protected Set<Integer> m_pipeIds;
		
		/**
		 * Creates a new instance of the property.
		 * @param pipe_ids The set of IDs corresponding to the outputs of
		 * the processor chain
		 */
		public OutputAlwaysTrue(Set<Integer> pipe_ids)
		{
			super(NAME);
			m_pipeIds = pipe_ids;
		}

		@Override
		public void printToFile(PrintStream ps)
		{
			int i = 0;
			for (int id : m_pipeIds)
			{
				if (i > 0)
				{
					ps.print(" & ");
				}
				ps.print("(G (ob_" + id + "[0] -> oc_" + id + "[0]))");
				i++;
			}
			ps.println(";");
		}
	}
	
	/**
	 * Stipulates that no queue in a processor chain should be full. A queue
	 * is considered full when the Boolean variable corresponding to its last
	 * position takes the value true. Given a list of such variables, the
	 * property stipulates that none of them may become true at any point in
	 * an execution. 
	 */
	protected static class NoFullQueues extends LTLPropertyProvider
	{
		/**
		 * The name of query "No full queues"
		 */
		public static final transient String NAME = "No full queues";
		
		/**
		 * The list of Boolean queue variables that must never be true
		 */
		protected ArrayVariable[] m_queueVars;
		
		/**
		 * Creates a new instance of the property.
		 * @param queue_vars The collection of Boolean queue variables that
		 * must never be true
		 */
		public NoFullQueues(Collection<ArrayVariable> queue_vars)
		{
			super(NAME);
			m_queueVars = new ArrayVariable[queue_vars.size()];
			int i = 0;
			for (ArrayVariable v : queue_vars)
			{
				m_queueVars[i++] = v;
			}
		}
		
		/**
		 * Creates a new instance of the property.
		 * @param queue_vars The list of Boolean queue variables that
		 * must never be true
		 */
		public NoFullQueues(ArrayVariable ... queue_vars)
		{
			super(NAME);
			m_queueVars = queue_vars;
		}

		@Override
		public void printToFile(PrintStream ps)
		{
			if (m_queueVars.length == 0)
			{
				ps.print("TRUE;");
				return;
			}
			ps.print("! (F (");
			for (int i = 0; i < m_queueVars.length; i++)
			{
				if (i > 0)
				{
					ps.print(" | ");
				}
				ps.print(m_queueVars[i].getName() + "[" + (m_queueVars[i].getDimension() - 1) + "]");
			}
			ps.println("));");
		}
	}
}
