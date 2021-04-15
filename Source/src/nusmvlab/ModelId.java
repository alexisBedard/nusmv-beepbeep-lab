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

import static nusmvlab.BeepBeepModelProvider.K;
import static nusmvlab.ModelProvider.DOMAIN_SIZE;
import static nusmvlab.ModelProvider.QUERY;
import static nusmvlab.ModelProvider.QUEUE_SIZE;
import static nusmvlab.PropertyProvider.PROPERTY;

import ca.uqac.lif.labpal.Region;

public class ModelId
	{
		protected String m_name;
		
		protected int m_queueSize;
		
		protected int m_domainSize;
		
		protected int m_k;
		
		protected String m_property;
		
		public ModelId(NuSMVExperiment e)
		{
			super();
			m_name = e.readString(QUERY);
			m_queueSize = e.readInt(QUEUE_SIZE);
			m_domainSize = e.readInt(DOMAIN_SIZE);
			m_property = e.readString(PROPERTY);
			m_k = e.readInt(K);
		}
		
		public ModelId(Region r)
		{
			super();
			m_name = r.getString(QUERY);
			m_queueSize = r.getInt(QUEUE_SIZE);
			m_domainSize = r.getInt(DOMAIN_SIZE);
			m_property = r.getString(PROPERTY);
			m_k = r.getInt(K);
		}
		
		@Override
		public int hashCode()
		{
			return m_name.hashCode();
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (o == null || !(o instanceof ModelId))
			{
				return false;
			}
			ModelId m = (ModelId) o;
			if (m.m_k != m_k || m.m_queueSize != m_queueSize || m.m_domainSize != m_domainSize
					|| m.m_name.compareTo(m_name) != 0)
			{
				return false;
			}
			if (m_name.contains("comparison") && m_property.compareTo(m.m_property) != 0)
			{
				return false;
			}
			return true;
		}
	}