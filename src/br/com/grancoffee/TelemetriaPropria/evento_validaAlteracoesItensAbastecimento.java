package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_validaAlteracoesItensAbastecimento implements EventoProgramavelJava{
	
	/**
	 * 08/10/20 15:41 esse objeto valida se uma tecla da tela Retornos Abastecimento que j� foi ajustada est� sendo alterada e tbm faz o calculo para o campo Saldo Ap�s.
	 * 17/10/21 vs 1.5 Alterada a valida��o para considerar apenas altera��es de teclas onde a contagem foi > 0
	 * 18/10/21 vs 1.6 Altera��o no objeto para, se n�o era uma visita com contagem, por�m algu� insere informa��es de contagem, ele faz os calculos e come�a a adotar aquela visita como uma de contagem.
	 * 26/10/21 vs 1.7 Insere nos c�lculos a retirada dos retornos que n�o devem entrar nos calculos. Todos onde o campo REDUZESTOQUE da AD_MOTIVOSRETORNO esteja como "N".
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
		DynamicVO VO = (DynamicVO) arg0.getVo();
		VO.setProperty("QTDRETORNO", new BigDecimal(0));
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {	
		valida(arg0);
		start(arg0);		
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		
		BigDecimal produto = newVO.asBigDecimal("CODPROD");
		String tecla = newVO.asString("TECLA");
		BigDecimal contagem = BigDecimalUtil.getValueOrZero(newVO.asBigDecimal("CONTAGEM"));
		BigDecimal retorno = BigDecimalUtil.getValueOrZero(newVO.asBigDecimal("QTDRETORNO"));
		BigDecimal id = newVO.asBigDecimal("ID");
		BigDecimal diferenca = null;
		BigDecimal saldoAntes = newVO.asBigDecimal("SALDOANTERIOR");
		BigDecimal qtdpedido = BigDecimalUtil.getValueOrZero(newVO.asBigDecimal("QTDPEDIDO"));
		BigDecimal numos = BigDecimalUtil.getValueOrZero((BigDecimal) NativeSql.getColumnValue("NUMOS", "AD_RETABAST", "ID ="+id));
		BigDecimal retornoParaCalculo = null;
		BigDecimal saldoapos = null;
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		if(numos!=null && numos.intValue()>0 && oldVO.asBigDecimal("SALDOANTERIOR") !=null) {
			
//			if(contagem.intValue()==0) {
//				DynamicVO contgaemVO = getContagem(numos, tecla, produto);
//				if(contgaemVO!=null) {
//					contagem = BigDecimalUtil.getValueOrZero(contgaemVO.asBigDecimal("QTDCONTAGEM"));
//				}		
//			}

			
			BigDecimal saldoesperado = saldoAntes.add(qtdpedido);
			BigDecimal retornosAhSeremIgnorados = getRetornosAhSeremIgnorados(id,produto,tecla);
			retornoParaCalculo = retorno.subtract(retornosAhSeremIgnorados);
			
			if(validaSeHouveContagem(id)) { //houve contagem
				
				BigDecimal conteretorno = contagem.add(retornoParaCalculo);
				diferenca = conteretorno.subtract(saldoesperado);
				saldoapos = contagem;
				
			}else { //n�o houve contagem
				
				if (newVO.asBigDecimal("CONTAGEM") != oldVO.asBigDecimal("CONTAGEM")) { //se a contagem for alterada
					if(newVO.asBigDecimal("CONTAGEM").intValue()>0) { //se for maior que zero
						
						BigDecimal conteretorno = contagem.add(retornoParaCalculo);
						diferenca = conteretorno.subtract(saldoesperado);
						saldoapos = contagem;
						marcaVisitaComoTendoContagem(id);
					}
				}else {
					diferenca = new BigDecimal(0);
					saldoapos = saldoesperado.subtract(retornoParaCalculo);
				}
				
			}
			
			//alterar os dados
			
			newVO.setProperty("DIFERENCA", diferenca);
			newVO.setProperty("SALDOAPOS", saldoapos);
			
		}
		
		/*
		 * if(numos!=null && numos.intValue()>0 && saldoAntes!=null) {
			throw new Error("\nProduto: "+produto+
					"\ntecla: "+tecla+
					"\ncontagem: "+contagem+
					"\nretorno: "+retorno+
					"\nid: "+id+
					"\ndiferenca: "+diferenca+
					"\nsaldoAntes: "+saldoAntes+
					"\nqtdpedido: "+qtdpedido+
					"\nnumos: "+numos+
					"\nretornoParaCalculo: "+retornoParaCalculo+
					"\nsaldoapos: "+saldoapos);
		}
		 */
		 
	}
	
//	private DynamicVO getContagem(BigDecimal numos, String tecla, BigDecimal produto) throws Exception {
//		JapeWrapper DAO = JapeFactory.dao("AD_APPCONTAGEM");
//		DynamicVO VO = DAO.findOne("this.NUMOS=? AND this.CODPROD=? AND this.TECLA=?",
//				new Object[] { numos, produto, tecla });
//		return VO;
//	}
	
	private BigDecimal getRetornosAhSeremIgnorados(BigDecimal id, BigDecimal produto, String tecla) {
		BigDecimal qtd = null;
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT NVL(SUM(QTD),0) AS QTD FROM AD_PRODRETABAST WHERE ID="+id+" AND CODPROD="+produto+" AND TECLA='"+tecla+"' AND IDRETORNO IN (SELECT ID FROM AD_MOTIVOSRETORNO WHERE REDUZESTOQUE='N')");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				BigDecimal count = contagem.getBigDecimal("QTD");
				if(count!=null) {
					qtd = count;
				}
			}
		} catch (Exception e) {
			salvarException(
					"[getRetornosAhSeremIgnorados] n�o foi poss�vel verificar a quantidade de retornos a serem ignorados. id " + id + "produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}
		
		if(qtd==null) {
			qtd = new BigDecimal(0);
		}
		
		return qtd;
	}
	
	
	private void marcaVisitaComoTendoContagem(BigDecimal id){
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_RETABAST",
					"this.ID=?", new Object[] { id }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CONTAGEM", "S");

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[marcaVisitaComoTendoContagem] n�o foi poss�vel marcar a visita como pendente: "+id+"\n"+e.getMessage()+e.getCause());
		}
	}
	
	private void valida(PersistenceEvent arg0) throws PersistenceException {
		DynamicVO VO = (DynamicVO) arg0.getOldVO();
		if(VO!=null) {
			String ajustado = VO.asString("AJUSTADO");
			
			if("S".equals(ajustado)) {
				throw new PersistenceException("<br/><br/><br/> <b>N�o � possivel alterar uma tecla j� ajustada!</b> <br/><br/><br/>");
			}
		}	
	}
	
	private boolean validaSeHouveContagem(BigDecimal id) throws Exception {
		boolean valida=false;
		JapeWrapper DAO = JapeFactory.dao("AD_RETABAST");
		DynamicVO VO = DAO.findOne("ID=?",new Object[] { id });
		if(VO!=null) {
			String contagem = VO.asString("CONTAGEM");
			if("S".equals(contagem)) {
				valida=true;
			}
		}
		
		return valida;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_validaAlteracoesItensAbastecimento");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui n�o tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
}
