package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

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
	 * 08/10/20 15:41 esse objeto valida se uma tecla da tela Retornos Abastecimento que já foi ajustada está sendo alterada e tbm faz o calculo para o campo Saldo Após.
	 * 17/10/21 vs 1.5 Alterada a validação para considerar apenas alterações de teclas onde a contagem foi > 0
	 * 18/10/21 vs 1.6 Alteração no objeto para, se não era uma visita com contagem, porém algué insere informações de contagem, ele faz os calculos e começa a adotar aquela visita como uma de contagem.
	 * 26/10/21 vs 1.7 Insere nos cálculos a retirada dos retornos que não devem entrar nos calculos. Todos onde o campo REDUZESTOQUE da AD_MOTIVOSRETORNO esteja como "N".
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
		BigDecimal contagem = newVO.asBigDecimal("CONTAGEM");
		BigDecimal retorno = newVO.asBigDecimal("QTDRETORNO");
		BigDecimal id = newVO.asBigDecimal("ID");
		BigDecimal diferenca = null;
		BigDecimal saldoAntes = newVO.asBigDecimal("SALDOANTERIOR");
		BigDecimal qtdpedido = newVO.asBigDecimal("QTDPEDIDO");
		BigDecimal numos = null;
		BigDecimal retornoParaCalculo = null;
		BigDecimal saldoapos = null;
		
		DynamicVO ad_RETABAST = getAD_RETABAST(id);
		if(ad_RETABAST!=null) {
			numos = ad_RETABAST.asBigDecimal("NUMOS");
		}
				
		if(numos!=null && saldoAntes!=null) {
			
			if(contagem==null) {
				contagem = new BigDecimal(0);
			}
			
			if(retorno==null) {
				retorno = new BigDecimal(0);
			}
			
			if(qtdpedido==null) {
				qtdpedido = new BigDecimal(0);
			}
			
			BigDecimal saldoesperado = saldoAntes.add(qtdpedido);
			BigDecimal retornosAhSeremIgnorados = getRetornosAhSeremIgnorados(numos,produto,tecla);
			retornoParaCalculo = retorno.subtract(retornosAhSeremIgnorados);
			
			if(validaSeHouveContagem(id)) { //houve contagem
				
				BigDecimal conteretorno = contagem.add(retornoParaCalculo);
				diferenca = conteretorno.subtract(saldoesperado);
				saldoapos = contagem;
				
			}else { //não houve contagem
				
				DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
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

	}
	
	private BigDecimal getRetornosAhSeremIgnorados(BigDecimal numos, BigDecimal produto, String tecla) {
		BigDecimal qtd = null;
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT SUM(QTD) AS QTD from AD_APPRETORNO WHERE NUMOS="+numos+" AND CODPROD="+produto+" AND TECLA='"+tecla+"' AND IDRETORNO IN (SELECT ID FROM AD_MOTIVOSRETORNO WHERE REDUZESTOQUE='N')");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				BigDecimal count = contagem.getBigDecimal("QTD");
				if(count!=null) {
					qtd = count;
				}
			}
		} catch (Exception e) {
			salvarException(
					"[getRetornosAhSeremIgnorados] não foi possível verificar a quantidade de retornos a serem ignorados. numos " + numos
							+ e.getMessage() + "\n" + e.getCause());
		}
		
		if(qtd==null) {
			qtd = new BigDecimal(0);
		}
		
		return qtd;
	}
	
	private DynamicVO getAD_RETABAST(BigDecimal id) {
		DynamicVO ret = null;
		try {
			JapeWrapper DAO = JapeFactory.dao("AD_RETABAST");
			DynamicVO VO = DAO.findOne("ID=?",new Object[] { id });
			if(VO!=null) {
				VO = ret;
			}
		} catch (Exception e) {
			salvarException("[getAD_RETABAST] não foi possível verificar a AD_RETABAST ID: "+id+"\n"+e.getMessage()+e.getCause());
		}
		return ret;
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
			salvarException("[marcaVisitaComoTendoContagem] não foi possível marcar a visita como pendente: "+id+"\n"+e.getMessage()+e.getCause());
		}
	}
	
	private void valida(PersistenceEvent arg0) throws PersistenceException {
		DynamicVO VO = (DynamicVO) arg0.getOldVO();
		if(VO!=null) {
			String ajustado = VO.asString("AJUSTADO");
			
			if("S".equals(ajustado)) {
				throw new PersistenceException("<br/><br/><br/> <b>Não é possivel alterar uma tecla já ajustada!</b> <br/><br/><br/>");
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
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
}
