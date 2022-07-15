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

import ca.uqac.lif.jerrydog.CallbackResponse;
import ca.uqac.lif.jerrydog.CallbackResponse.ContentType;
import ca.uqac.lif.labpal.server.LabPalServer;
import ca.uqac.lif.labpal.server.LaboratoryCallback;
import ca.uqac.lif.labpal.util.FileHelper;
import com.sun.net.httpserver.HttpExchange;
import java.net.URI;

/**
 * Special callback for the LabPal server to fetch images from the
 * internal JAR.
 */
public class InnerFileCallback extends LaboratoryCallback
{
  public InnerFileCallback(LabPalServer server)
  {
    super(server, Method.GET, "/resource");
  }

  @Override
  public CallbackResponse process(HttpExchange he)
  {
    CallbackResponse cbr = new CallbackResponse(he);
    URI uri = he.getRequestURI();
    String path = uri.getPath();
    path = path.substring(1); // Remove first slash
    byte[] file_contents = FileHelper.internalFileToBytes(getServer().getLaboratory().getClass(), path);
    if (file_contents == null)
    {
      cbr.setCode(CallbackResponse.HTTP_NOT_FOUND);
    }
    else
    {
      cbr.setContentType(ContentType.PNG);
      cbr.setCode(CallbackResponse.HTTP_OK);
      cbr.setContents(file_contents);
    }
    return cbr;
  }

}