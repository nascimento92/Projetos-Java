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

public class evento_lote_new implements EventoProgramavelJava {
	
	String tipo="";
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
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		tipo="insert";
		start(arg0,tipo);	
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		tipo="update";
		start(arg0,tipo);	
	}
	
	public void start(PersistenceEvent arg0, String tipo) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		BigDecimal nroUnico = VO.asBigDecimal("NUNOTA");
		String lote = VO.asString("CONTROLE");
		
		//String movimentaBem = "";
		BigDecimal empresa = null;
		String tipoMovimento = "";
		String controle = "";
		
		try {
			//movimentaBem = verificaSeAhTopMovimentaBem(nroUnico);
			empresa = getTGFCAB(nroUnico).asBigDecimal("CODEMP");
			tipoMovimento =  verificaSeAhTopEhUmaDevolucao(nroUnico);
			controle= verificaSeOhProdutoEhControladoPorLote(produto);			
		} catch (Exception e) {
			System.out.println("**[EVENTO_VALIDA_LOTE_TGFITE] NAO FOI OBTER OS DADOS ** "+e.getMessage());
			e.printStackTrace();
		}
		
		//se for uma compra
		if("C".equals(tipoMovimento) && "insert".equals(this.tipo)) {
			if("L".equals(controle)) {
				if(getTGFPRO(produto).asBigDecimal("CODGRUPOPROD").intValue()==500104) {
					if(validaLoteValido(lote,empresa)) {
						throw new PersistenceException(
								"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
								"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>ERRO - LOTE JA EXISTE NO SISTEMA.</b></font>\n\n\n <b>O lote informado ("+lote+"), para a empresa ("+empresa+") já existe no sistema, não pode ser informado novamente!</b>");
					}
				}
			}
		}else if("V".equals(tipoMovimento) || "D".equals(tipoMovimento)) {
			if("L".equals(controle)) {
				if(lote!="99999") {
					if(getTGFPRO(produto).asBigDecimal("CODGRUPOPROD").intValue()==500104) {
						
						if(!validaLoteValido(lote,empresa)) {
							throw new PersistenceException(
									"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
									"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>ERRO - LOTE INVÁLIDO.</b></font>\n\n\n <b>O lote informado ("+lote+") não existe no sistema ou não pertence a empresa da nota, entrar em contato com o setor patrimonial!</b>");

						}else {
							
							if(!validaProdutoELote(lote,produto)) {
								throw new PersistenceException(
										"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
										"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>ERRO - LOTE X PRODUTO.</b></font>\n\n\n <b>O lote informado ("+lote+") não pertence a este produto!</b>");
							}
							
						}
					}
				}
			}
		}		
	}
		
	//---------------------------------------------------------
	
	public String verificaSeAhTopMovimentaBem(BigDecimal numeroUnico) throws Exception {
		DynamicVO tgfcab = getTGFCAB(numeroUnico);
		BigDecimal top = tgfcab.asBigDecimal("CODTIPOPER");
		DynamicVO tgftop = ComercialUtils.getTipoOperacao(top);
		String atualizaBem = tgftop.asString("ATUALBEM");

		return atualizaBem;
	}
	
	public DynamicVO getTGFCAB(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?", new Object[] { nunota });
		return VO;
	}
	
	public String verificaSeAhTopEhUmaDevolucao(BigDecimal numeroUnico) throws Exception {
		DynamicVO tgfcab = getTGFCAB(numeroUnico);
		BigDecimal top = tgfcab.asBigDecimal("CODTIPOPER");
		DynamicVO tgftop = ComercialUtils.getTipoOperacao(top);
		String atualizaBem = tgftop.asString("TIPMOV");

		return atualizaBem;
	}
	
	public String verificaSeOhProdutoEhControladoPorLote(BigDecimal codprod) throws Exception {
		DynamicVO tgfpro = getTGFPRO(codprod);
		String controleEstoque = tgfpro.asString("TIPCONTEST");
		return controleEstoque;
	}
	
	public DynamicVO getTGFPRO(BigDecimal codprod) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { codprod });
		return VO;
	}
	
	public boolean validaLoteValido(String controle, BigDecimal empresa) throws Exception {
		boolean valida=false;
		JapeWrapper DAO = JapeFactory.dao("Estoque");
		DynamicVO VO = DAO.findOne("CONTROLE=? AND CODEMP=?",new Object[] { controle,empresa});
		if(VO!=null) {
			valida=true;
		}
		return valida;
	}
	
	public boolean validaEmpresaDoLote(String lote, BigDecimal empresa) throws Exception {
		boolean valida=false;
		JapeWrapper DAO = JapeFactory.dao("Estoque");
		DynamicVO VO = DAO.findOne("CONTROLE=? and CODEMP=?",new Object[] { lote, empresa });
		if(VO!=null) {
			valida=true;
		}
		return valida;
	}
	
	public boolean validaProdutoELote(String controle, BigDecimal produto) throws Exception {
		boolean valida=false;
		JapeWrapper DAO = JapeFactory.dao("Estoque");
		DynamicVO VO = DAO.findOne("CONTROLE=? AND CODPROD=?",new Object[] { controle,produto});
		if(VO!=null) {
			valida=true;
		}
		return valida;
	}
	
}
