package org.terrier.querying;

import java.util.ArrayList;
import java.util.List;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.MatchingQueryTerms.MatchingTerm;
import org.terrier.matching.indriql.PhraseTerm;
import org.terrier.matching.indriql.SingleQueryTerm;
import org.terrier.matching.indriql.UnorderedWindowTerm;
import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.dependence.pBiL;
import org.terrier.querying.parser.Query.QTPBuilder;

@ProcessPhaseRequisites(ManagerRequisite.MQT)
public class DependenceModelPreProcess implements Process {
	
	static final String DEFAULT_DEPENDENCE_WEIGHTING_MODEL = pBiL.class.getName();
	public static final String CONTROL_MODEL = "dependencemodel";
	public static final String CONTROL_MODEL_PARAM = "dependencemodelparam";
	
	Double param = null;
	
	@Override
	public void process(Manager manager, SearchRequest q) {
		String modelName = q.getControl(CONTROL_MODEL);
		if (modelName == null)
			modelName = DEFAULT_DEPENDENCE_WEIGHTING_MODEL;
		
		String paramValue = q.getControl(CONTROL_MODEL_PARAM);
		param = paramValue != null ? Double.parseDouble(paramValue) : null;	
		this.process(((Request)q).getMatchingQueryTerms(), modelName);
	}
	
	WeightingModel getModel(String name, int ngramLength) {
		if (! name.contains("."))
			name = "org.terrier.matching.models.dependence." + name;
		WeightingModel rtr = null;
		try{
			rtr = Class.forName(name).asSubclass(WeightingModel.class).getConstructor(Integer.TYPE).newInstance(ngramLength);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		if (param != null)
			rtr.setParameter(param);
		return rtr;
	}
	
	public void process(MatchingQueryTerms mqt, String modelName)
	{
		assert mqt != null;
		List<String> queryTerms = new ArrayList<>();
		for(MatchingTerm e : mqt)
		{
			if (! ( e.getKey() instanceof SingleQueryTerm))
				continue;
			queryTerms.add(e.getKey().toString());
		}
		
		if (queryTerms.size() < 2)
			return;
		
		List<MatchingTerm> newEntries = new ArrayList<>();
		
		//#1
		for(int i=0;i<queryTerms.size()-1;i++)
		{
			QTPBuilder qtp = QTPBuilder.of(new PhraseTerm(new String[]{queryTerms.get(i), queryTerms.get(i+1)}));
			qtp.setWeight(0.1d);
			qtp.addWeightingModel(getModel(modelName,2));
			newEntries.add(qtp.build());
		}
		
		//#uw8
		for(int i=0;i<queryTerms.size()-1;i++)
		{
			QTPBuilder qtp = QTPBuilder.of(new UnorderedWindowTerm(new String[]{queryTerms.get(i), queryTerms.get(i+1)}, 8));
			qtp.setWeight(0.1d);
			qtp.addWeightingModel(getModel(modelName,8));
			newEntries.add(qtp.build());
		}
		
		//#uw12
		QTPBuilder qtp = QTPBuilder.of(new UnorderedWindowTerm(queryTerms.toArray(new String[queryTerms.size()]), 12));
		qtp.setWeight(0.1d);
		qtp.addWeightingModel(getModel(modelName,12));
		newEntries.add(qtp.build());
		
		//finally add the new entries
		mqt.addAll(newEntries);
	}

	

	@Override
	public String getInfo() {
		return this.getClass().getSimpleName();
	}
	
}
