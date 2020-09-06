package Travas;

import java.text.Normalizer;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class valida_caracteres_especiais implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);		
	}
	
	//1.0
	private void start(PersistenceEvent arg0) throws Exception {
		String tabela = descobreAhTabela(arg0);
		
		if("TGFPAR".equals(tabela)) {
			TGFPAR(arg0);
		}else if("TSIEND".equals(tabela)) {
			TSIEND(arg0);
		}else if("TSIBAI".equals(tabela)) {
			TSIBAI(arg0);
		}else if("TSICID".equals(tabela)) {
			TSICID(arg0);
		}else if("AD_PLANTAS".equals(tabela)) {
			PLANTAS(arg0);
		}

	}
	
	//1.1
	private String descobreAhTabela(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String instancia = VO.getValueObjectID();
		int qtdStrings = instancia.indexOf(".");
		String instanciaNew = instancia.substring(0,qtdStrings);
		
		JapeWrapper DAO = JapeFactory.dao("Instancia");
		DynamicVO VO2 = DAO.findOne("NOMEINSTANCIA=?",new Object[] { instanciaNew });
		
		String tabela = VO2.asString("NOMETAB");
		
		return tabela;
	}
	
	//1.2
	private void TGFPAR(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		String nomeParceiro = VO.asString("NOMEPARC");
		if(nomeParceiro!=null) {
			String novoNomeParceiro = removeCaracteresEspeciais(nomeParceiro);
			VO.setProperty("NOMEPARC", novoNomeParceiro);	
		}
		
		String razaoSocial = VO.asString("RAZAOSOCIAL");
		if(razaoSocial!=null) {
			String novaRazaoSocial = removeCaracteresEspeciais(razaoSocial);
			VO.setProperty("RAZAOSOCIAL", novaRazaoSocial);
		}
		
		String numeroEndereco = VO.asString("NUMEND");
		if(numeroEndereco!=null) {
			String novoNumeroEndereco = removeCaracteresEspeciais(numeroEndereco);
			VO.setProperty("NUMEND", novoNumeroEndereco);
		}
		
		String complemento = VO.asString("COMPLEMENTO");
		if(complemento!=null) {
			String novoComplemento = removeCaracteresEspeciais(complemento);
			VO.setProperty("COMPLEMENTO", novoComplemento);	
		}
		
	}
	
	//1.3
	private void TSIEND(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		String endereco = VO.asString("NOMEEND");
		if(endereco!=null) {
			String novoEndereco = removeCaracteresEspeciais(endereco);
			VO.setProperty("NOMEEND", novoEndereco);	
		}
	}
	
	//1.4
	private void TSIBAI(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		String bairro = VO.asString("NOMEBAI");
		if(bairro!=null) {
			String novoBairro = removeCaracteresEspeciais(bairro);
			VO.setProperty("NOMEBAI", novoBairro);	
		}
	}
	
	//1.5
	private void TSICID(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		String cidade = VO.asString("NOMECID");
		if(cidade!=null) {
			String novaCidade = removeCaracteresEspeciais(cidade);
			VO.setProperty("NOMECID", novaCidade);	
		}
	}
	
	//1.6
	private void PLANTAS(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();

		String planta = VO.asString("NOMEPLAN");
		if (planta != null) {
			String novaPLanta = removeCaracteresEspeciais(planta);
			VO.setProperty("NOMEPLAN", novaPLanta);
		}
		
		String enderecoPLanta = VO.asString("ENDPLAN");
		if (enderecoPLanta != null) {
			String novoEnderecoPlanta = removeCaracteresEspeciais(enderecoPLanta);
			VO.setProperty("ENDPLAN", novoEnderecoPlanta);
		}
	}
	
	//METODO AUXILIAR
	public static String removeCaracteresEspeciais(String str) {
	    String semAcento = Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	    String semCaracteresEspeciais=Normalizer.normalize(semAcento, Normalizer.Form.NFD).replaceAll("[(|!?¨*°;:{}$#%^~&'\"\\<>)]", "");
	    String stringFInal = semCaracteresEspeciais.trim().replaceAll("\\s+", " ").toUpperCase();
	   
	    return stringFInal; 
	}

}
