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

/**
 * Generates a "dummy" NuSMV model that does not correspond to any particular
 * problem. This class is only used to test the implementation of the lab and
 * should not be used in the final experiments.
 */
public class DummyModelProvider extends ModelProvider
{
	/**
	 * The name of this model provider
	 */
	public static final transient String NAME = "Dummy";
	
	/**
	 * Creates a new dummy model provider.
	 * @param queue_size
	 * @param domain_size
	 */
	public DummyModelProvider(int queue_size, int domain_size)
	{
		super(NAME, queue_size, domain_size);
	}

	@Override
	public void printToFile(PrintStream ps)
	{
		ps.println("MODULE main");
		ps.println("VAR");
		ps.println("  x : 0.." + m_domainSize + ";");
		ps.println("INIT");
		ps.println("  x = 0;");
		ps.println("TRANS");
		ps.println("  next(x) = x + 1;");
	}
}
