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

/**
 * Provider that returns a CTL formula or a list of CTL formulas.
 */
public abstract class CTLPropertyProvider implements PropertyProvider
{
	/**
	 * The name given to the property
	 */
	protected transient String m_name;
	
	/**
	 * Creates a new LTL property provider.
	 * @param name The name given to the property
	 */
	public CTLPropertyProvider(String name)
	{
		super();
		m_name = name;
	}

	@Override
	public void fillExperiment(NuSMVExperiment e) 
	{
		e.describe(PROPERTY, "The CTL property that is verified on the model");
		e.writeInput(PROPERTY, m_name);
	}

	@Override
	public final Logic getLogic() 
	{
		return Logic.CTL;
	}
}
