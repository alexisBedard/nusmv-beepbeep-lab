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

public class MainLab extends Laboratory
{
	@Override
	public void setup()
	{
		// Lab metadata
		setTitle("A benchmark for NuSMV extensions to BeepBeep 3");
	}
	
	public static void main(String[] args)
	{
		// Nothing else to do here
		MainLab.initialize(args, MainLab.class);
	}
}
