package br.com.NOTAS;

import java.math.BigDecimal;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.ComercialUtils;

public class evento_valida_lote_tgfite implements EventoProgramavelJava {

	/**
	 * @author gabriel.nascimento
	 * 
	 */
		
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
		start(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();	
		validaDados(VO);
	}
	
	private void validaDados(DynamicVO VO) throws Exception {
		
		String controle = "";
		BigDecimal grupoProduto= null;
		String lote = "";
		String tipoMovimento = "";
		BigDecimal empresa = null;
		String validaControlePatrimonial = "N";
		
		try {
			
			controle= getTGFPRO(VO.asBigDecimal("CODPROD")).asString("TIPCONTEST");	
			grupoProduto = getTGFPRO(VO.asBigDecimal("CODPROD")).asBigDecimal("CODGRUPOPROD");
			validaControlePatrimonial = getTGFGRU(grupoProduto).asString("AD_GESTAOPATRIMONIAL");
			lote = VO.asString("CONTROLE");
			tipoMovimento =  tipMov(VO.asBigDecimal("NUNOTA"));
			empresa = getTGFCAB(VO.asBigDecimal("NUNOTA")).asBigDecimal("CODEMP");
			
		} catch (Exception e) {
			System.out.println("**[EVENTO_VALIDA_LOTE_TGFITE] NAO FOI OBTER OS DADOS ** "+e.getMessage());
			e.printStackTrace();
		}
		
		//validacoes em comum
		if("L".equals(controle)) {
			if("S".equals(validaControlePatrimonial)) {
				if(lote!="99999") {
					
					if("P".equals(tipoMovimento)) {
						if(!verificaLote(lote,empresa)) {
							throw new PersistenceException(
									"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
									"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>ERRO - LOTE INVÁLIDO.</b></font>\n\n\n <b>O lote informado ("+lote+") não existe no sistema! ou não pertence a empresa ("+empresa+")</b>");
						}
					}	
				}
			}
		}
	}
		
	private DynamicVO getTGFPRO(BigDecimal codprod) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { codprod });
		return VO;
	}
	
	private String tipMov(BigDecimal numeroUnico) throws Exception {
		DynamicVO tgfcab = getTGFCAB(numeroUnico);
		BigDecimal top = tgfcab.asBigDecimal("CODTIPOPER");
		DynamicVO tgftop = ComercialUtils.getTipoOperacao(top);
		String atualizaBem = tgftop.asString("TIPMOV");

		return atualizaBem;
	}
	
	private DynamicVO getTGFCAB(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?", new Object[] { nunota });
		return VO;
	}
	
	private boolean verificaLote(String lote, BigDecimal empresa) throws Exception {
		boolean valida=false;
		JapeWrapper DAO = JapeFactory.dao("Estoque");
		DynamicVO VO = DAO.findOne("CONTROLE=? AND CODEMP=?",new Object[] { lote,empresa });
		if(VO!=null) {
			valida=true;
		}
		return valida;
	}
	
	private DynamicVO getTGFGRU(BigDecimal grupo) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GrupoProduto");
		DynamicVO VO = DAO.findOne("CODGRUPOPROD=?", new Object[] { grupo });
		return VO;
	}

}
