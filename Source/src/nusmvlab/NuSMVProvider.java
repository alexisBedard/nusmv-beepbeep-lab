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

import java.io.IOException;
import java.io.PrintStream;

/**
 * Interface for any object that can write a parts of a NuSMV file to a
 * {@link PrintStream}. The provider can also populate an experiment with
 * parameters pertaining to the things it writes to the file.
 */
public interface NuSMVProvider
{
	/**
	 * Writes a NuSMV file to a PrintStream.
	 * @param ps The stream to write to
	 */
	public void printToFile(/*@ non_null @*/ PrintStream ps) throws IOException;
	
	/**
	 * Writes parameters pertaining to the model into an experiment.
	 * @param e The experiment to write to
	 */
	public void fillExperiment(/*@ non_null @*/ NuSMVExperiment e);
}
