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

public interface PropertyProvider extends NuSMVProvider
{
	/**
	 * The name of parameter "Property"
	 */
	public static final String PROPERTY = "Property";
	
	/**
	 * The type of formula this provider writes.
	 */
	public enum Logic {LTL, CTL}
	
	/**
	 * Gets the type of formula this provider writes (CTL or LTL).
	 * @return The formula type
	 */
	public Logic getLogic();
}
