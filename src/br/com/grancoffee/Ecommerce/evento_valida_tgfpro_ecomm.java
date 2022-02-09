package br.com.grancoffee.Ecommerce;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class evento_valida_tgfpro_ecomm implements EventoProgramavelJava{

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
		String tipo = "Insert";
		start(arg0,tipo);		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		String tipo = "Update";
		start(arg0,tipo);		
	}
	
	private void start(PersistenceEvent arg0, String tipo) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String integra = VO.asString("AD_INT_VTEX");
		
		String nome = VO.asString("AD_NOMEPRDLV"); //[VTEX] Nome
		String unidade = VO.asString("AD_UNIDADELV"); //[VTEX] Unidade
		BigDecimal altura = VO.asBigDecimal("AD_ALTURALV");//[VTEX] Altura (em Centímetro)
		BigDecimal largura = VO.asBigDecimal("AD_LARGURALV");//[VTEX] Largura (em Centímetro)
		BigDecimal comprimento = VO.asBigDecimal("AD_COMPRIMENTOLV");//[VTEX] Comprimento (em Centímetro)
		BigDecimal peso = VO.asBigDecimal("AD_PESOLV");//[VTEX] Peso (em gramas)
		BigDecimal marca = VO.asBigDecimal("AD_IDMARCAECOM"); //[VTEX] Marca
		BigDecimal categoria = VO.asBigDecimal("AD_IDCATECOMM");//[VTEX] Categoria
		String codigoBarras = VO.asString("AD_CODBARRA");//[VTEX] Código de barras
		String descricaoCurta = VO.asString("AD_DESCURTALV");//[VTEX] Descrição curta
		String descricaoLonga = VO.asString("AD_DESCLONGALV");//[VTEX] Descrição longa
		
		if("Insert".equals(tipo)) {
			if("S".equals(integra)) {

				validacao(nome,unidade,altura,largura,comprimento,peso,marca,categoria,codigoBarras,descricaoCurta,descricaoLonga);	
			}
		}
		
		if("Update".equals(tipo)) {
			DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
			String integraOld = oldVO.asString("AD_INT_VTEX");
			
			if("S".equals(integraOld) && "N".equals(integra)) {
				throw new Error("<br/><b>ATENÇÃO!</b><br/><br/>O Produto já foi integrado a VTEX não é possível desmarcar esta opção! <br/>");
			}
			
			if("S".equals(integra)) {
				validacao(nome,unidade,altura,largura,comprimento,peso,marca,categoria,codigoBarras,descricaoCurta,descricaoLonga);
			}
				
		}
		
	}
	
	private void validacao(String nome,String unidade,BigDecimal altura,BigDecimal largura,BigDecimal comprimento,BigDecimal peso,BigDecimal marca,BigDecimal categoria,String codigoBarras,String descricaoCurta,String descricaoLonga) {
		String erro = null;
		
		if(nome==null) {
			erro="\nCampo <b>[VTEX] Nome</b> não pode ficar vazio!";
		}
		
		if(unidade==null) {
			erro="\nCampo <b>[VTEX] Unidade</b> não pode ficar vazio!";
		}
		
		if(altura==null) {
			erro="\nCampo <b>[VTEX] Altura (em Centímetro)</b> não pode ficar vazio!";
		}
		
		if(largura==null) {
			erro="\nCampo <b>[VTEX] Largura (em Centímetro)</b> não pode ficar vazio!";
		}
		
		if(comprimento==null) {
			erro="\nCampo <b>[VTEX] Comprimento (em Centímetro)</b> não pode ficar vazio!";
		}
		
		if(peso==null) {
			erro="\nCampo <b>[VTEX] Peso (em gramas)</b> não pode ficar vazio!";
		}
		
		if(marca==null) {
			erro="\nCampo <b>[VTEX] Marca</b> não pode ficar vazio!";
		}
		
		if(categoria==null) {
			erro="\nCampo <b>[VTEX] Categoria</b> não pode ficar vazio!";
		}
		
		if(codigoBarras==null) {
			erro="\nCampo <b>[VTEX] Código de barras</b> não pode ficar vazio!";
		}
		
		if(descricaoCurta==null) {
			erro="\nCampo <b>[VTEX] Descrição curta</b> não pode ficar vazio!";
		}
		
		if(descricaoLonga==null) {
			erro="\nCampo <b>[VTEX] Descrição longa</b> não pode ficar vazio!";
		}
		
		if(erro!=null) {
			throw new Error("<br/><b>ATENÇÃO!</b><br/><br/>O Produto foi marcado para ser integrado no E-commerce VTEX, porém: <br/>"+erro);
		}
		
	}

}
