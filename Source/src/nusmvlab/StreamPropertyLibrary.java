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

import ca.uqac.lif.cep.smv.SmvModule.SmvVariable;
import ca.uqac.lif.labpal.Region;

import static nusmvlab.PropertyProvider.PROPERTY;

import java.io.PrintStream;
import java.util.Collection;

/**
 * Library that produces property providers based on the contents of a
 * region.
 */
public class StreamPropertyLibrary implements Library<PropertyProvider>
{	
	/**
	 * The name of query "Incrementing x"
	 */
	public static final transient String P_X_STAYS_NULL = "x stays null";
	
	/**
	 * The name of query "No full queues"
	 */
	public static final transient String P_NO_FULL_QUEUES = "No full queues";
	
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
		if (name == null)
		{
			return null;
		}
		if (name.compareTo(P_X_STAYS_NULL) == 0)
		{
			return new XStaysNull();
		}
		if (name.compareTo(P_NO_FULL_QUEUES) == 0)
		{
			if (model instanceof BeepBeepModelProvider)
			{
				BeepBeepModelProvider b_model = (BeepBeepModelProvider) model;;
				return new NoFullQueues(b_model.getQueueVariables());
			}
		}
		return null;
	}
	
	protected static class XStaysNull extends CTLPropertyProvider
	{
		public XStaysNull()
		{
			super(P_X_STAYS_NULL);
		}

		@Override
		public void printToFile(PrintStream ps)
		{
			ps.println("  AG (x = 0 -> AG (x = 0));");
		}
	}
	
	/**
	 * Stipulates that no queue in a processor chain should be full. A queue
	 * is considered full when the Boolean variable corresponding to its last
	 * position takes the value true. Given a list of such variables, the
	 * property stipulates that none of them may become true at any point in
	 * an execution. 
	 */
	protected static class NoFullQueues extends CTLPropertyProvider
	{
		/**
		 * The list of Boolean queue variables that must never be true
		 */
		protected SmvVariable[] m_queueVars;
		
		/**
		 * Creates a new instance of the property.
		 * @param queue_vars The collection of Boolean queue variables that
		 * must never be true
		 */
		public NoFullQueues(Collection<SmvVariable> queue_vars)
		{
			super(P_NO_FULL_QUEUES);
			m_queueVars = new SmvVariable[queue_vars.size()];
			int i = 0;
			for (SmvVariable v : queue_vars)
			{
				m_queueVars[i++] = v;
			}
		}
		
		/**
		 * Creates a new instance of the property.
		 * @param queue_vars The list of Boolean queue variables that
		 * must never be true
		 */
		public NoFullQueues(SmvVariable ... queue_vars)
		{
			super(P_NO_FULL_QUEUES);
			m_queueVars = queue_vars;
		}

		@Override
		public void printToFile(PrintStream ps)
		{
			if (m_queueVars.length == 0)
			{
				ps.print("TRUE");
				return;
			}
			ps.print("! (EF (");
			for (int i = 0; i < m_queueVars.length; i++)
			{
				if (i > 0)
				{
					ps.print(" | ");
				}
				ps.print(m_queueVars[i].getName() + "[" + (m_queueVars[i].getSize() - 1) + "]");
			}
			ps.println("));");
		}
	}
}
