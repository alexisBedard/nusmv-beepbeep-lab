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

import ca.uqac.lif.labpal.Region;

import static nusmvlab.PropertyProvider.PROPERTY;

import java.io.PrintStream;

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
	 * Creates a new instance of the library.
	 */
	public StreamPropertyLibrary()
	{
		super();
	}
	
	@Override
	public PropertyProvider get(Region r)
	{
		String name = r.getString(PROPERTY);
		if (name == null)
		{
			return null;
		}
		if (name.compareTo(P_X_STAYS_NULL) == 0)
		{
			return new XStaysNull();
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
			ps.println("  AG (x = 0 -> AF (x = 0));");
		}
	}
}
