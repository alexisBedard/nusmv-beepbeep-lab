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

import java.io.PrintStream;

import ca.uqac.lif.cep.Processor;

/**
 * Provides a NuSMV model based on a chain of BeepBeep processors.
 */
public class BeepBeepModelProvider extends ModelProvider
{	
	/**
	 * The processor at the start of the chain
	 */
	protected Processor m_start;
	
	/**
	 * Creates a new instance of model provider.
	 * @param start The processor instance at the start of the chain
	 * @param name A (textual) name given to the model in question 
	 * @param queue_size The size of the queues in the SMV model to generate
	 * @param domain_size The size of the domains in the SMV model to generate
	 */
	public BeepBeepModelProvider(Processor start, String name, int queue_size, int domain_size)
	{
		super(name, queue_size, domain_size);
		m_start = start;
	}
	
	@Override
	public void printToFile(PrintStream ps)
	{
		// TODO: produce SMV model from start processor and given params
	}

}
