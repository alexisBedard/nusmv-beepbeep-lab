package nusmvlab;

import static nusmvlab.BeepBeepModelProvider.K;
import static nusmvlab.ModelProvider.DOMAIN_SIZE;
import static nusmvlab.ModelProvider.QUERY;
import static nusmvlab.ModelProvider.QUEUE_SIZE;

import ca.uqac.lif.labpal.Region;

public class ModelId
	{
		protected String m_name;
		
		protected int m_queueSize;
		
		protected int m_domainSize;
		
		protected int m_k;
		
		public ModelId(NuSMVExperiment e)
		{
			super();
			m_name = e.readString(QUERY);
			m_queueSize = e.readInt(QUEUE_SIZE);
			m_domainSize = e.readInt(DOMAIN_SIZE);
			m_k = e.readInt(K);
		}
		
		public ModelId(Region r)
		{
			super();
			m_name = r.getString(QUERY);
			m_queueSize = r.getInt(QUEUE_SIZE);
			m_domainSize = r.getInt(DOMAIN_SIZE);
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
			return m.m_k == m_k && m.m_queueSize == m_queueSize && m.m_domainSize == m_domainSize
					&& m.m_name.compareTo(m_name) == 0;
		}
	}