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

public abstract class ModelProvider implements NuSMVProvider
{
	/**
	 * The size of the queues in the SMV model to generate
	 */
	protected int m_queueSize = 0;
	
	/**
	 * The cardinality of the domains in the SMV model to generate
	 */
	protected int m_domainSize = 0;
	
	/**
	 * The name of parameter "Query"
	 */
	public static final transient String QUERY = "Query";
	
	/**
	 * The name of parameter "Queue size"
	 */
	public static final transient String QUEUE_SIZE = "Queue size";
	
	/**
	 * The name of parameter "Domain size"
	 */
	public static final transient String DOMAIN_SIZE = "Domain size";
	
	/**
	 * The name of the processor chain
	 */
	protected String m_name;
	
	/**
	 * Creates a new instance of model provider.
	 * @param name A (textual) name given to the model in question 
	 * @param queue_size The size of the queues in the SMV model to generate
	 * @param domain_size The size of the domains in the SMV model to generate
	 */
	public ModelProvider(String name, int queue_size, int domain_size)
	{
		super();
		m_name = name;
		m_queueSize = queue_size;
		m_domainSize = domain_size;
	}
	
	@Override
	public void fillExperiment(NuSMVExperiment e)
	{
		e.describe(QUERY, "The chain of processors under study");
		e.setInput(QUERY, m_name);
		e.describe(QUEUE_SIZE, "The size of the queues in the generated SMV model");
		e.setInput(QUEUE_SIZE, m_queueSize);
		e.describe(DOMAIN_SIZE, "The cardinality of the domains in the generated SMV model");
		e.setInput(DOMAIN_SIZE, m_domainSize);
	}
}
