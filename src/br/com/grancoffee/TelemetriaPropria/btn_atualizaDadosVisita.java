package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_atualizaDadosVisita implements AcaoRotinaJava {
	int c = 0;
	int c2 = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		for(int i=0; i < linhas.length; i++) {
			start(linhas[i], arg0);
		}
		
		arg0.setMensagemRetorno("<br/><b>Atenção</b><br/><br/> Visitas que tiveram contagem e foram ajustadas: "+c+"\nVisitas que não tiveram contagem: "+c2+"<br/><br/>");
		
	}

	private void start(Registro linhas, ContextoAcao arg0) {
		BigDecimal numos = (BigDecimal) linhas.getCampo("NUMOS");
		BigDecimal id = (BigDecimal) linhas.getCampo("ID");
		String patrimonio = (String) linhas.getCampo("CODBEM");
		
		if (verificaSeHouveContagem(numos)) {
			c++;
			//arg0.setMensagemRetorno("Houve contagem, "+c+" visitas atualizadas.");
			verificaTeclas(numos,id,patrimonio);
		} else {
			c2++;
			//arg0.setMensagemRetorno("Não houve contagem!");
		}

	}

	private boolean verificaSeHouveContagem(BigDecimal numos) {
		boolean valida = false;

		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(QTDCONTAGEM) AS QTD FROM AD_APPCONTAGEM WHERE NUMOS=" + numos);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count >= 1) {
					valida = true;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		return valida;
	}
	
	private void verificaTeclas(BigDecimal numos, BigDecimal id, String patrimonio) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_ITENSRETABAST",
					"this.ID=?", new Object[] { id }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				String tecla = VO.asString("TECLA");
				BigDecimal produto = VO.asBigDecimal("CODPROD");
				BigDecimal qtdContagem = null;
				String ajustado = VO.asString("AJUSTADO");
				
				DynamicVO contagem = getContagem(numos,tecla,produto);
				if(contagem!=null) {
					qtdContagem = BigDecimalUtil.getValueOrZero(contagem.asBigDecimal("QTDCONTAGEM"));
				}else {
					qtdContagem = new BigDecimal(0);
				}
				
				if(!"S".equals(ajustado)) {
					BigDecimal saldoAntes = BigDecimalUtil.getValueOrZero(VO.asBigDecimal("SALDOANTERIOR"));
					BigDecimal qtdpedido = BigDecimalUtil.getValueOrZero(VO.asBigDecimal("QTDPEDIDO"));
					BigDecimal retorno = BigDecimalUtil.getValueOrZero(VO.asBigDecimal("QTDRETORNO"));
					
					BigDecimal diferenca = null;
					BigDecimal saldoapos = null;
					BigDecimal saldoesperado = saldoAntes.add(qtdpedido);
					BigDecimal retornosAhSeremIgnorados = getRetornosAhSeremIgnorados(id,produto,tecla);
					BigDecimal retornoParaCalculo = retorno.subtract(retornosAhSeremIgnorados);
					
					BigDecimal conteretorno = qtdContagem.add(retornoParaCalculo);
					diferenca = conteretorno.subtract(saldoesperado);
					saldoapos = qtdContagem;
										
					VO.setProperty("CONTAGEM", qtdContagem);
					VO.setProperty("DIFERENCA", diferenca);
					VO.setProperty("SALDOAPOS", saldoapos);
					
					atualizaHistorico(id,produto,tecla,qtdContagem,diferenca,saldoapos, retornoParaCalculo);
					
				}

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[verificaTeclas] nao foi possivel verificar as teclas! patrimonio: "+patrimonio+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void atualizaHistorico(BigDecimal id, BigDecimal produto, String tecla, BigDecimal contagem, BigDecimal diferenca, BigDecimal saldoapos, BigDecimal retorno) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_HISTRETABAST",
					"this.ID=? AND this.CODPROD=? AND this.TECLA=? ", new Object[] { id, produto,tecla }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CONTAGEM", contagem);
				VO.setProperty("DIFERENCA", diferenca);
				VO.setProperty("SALDOAPOS", saldoapos);
				VO.setProperty("QTDRETORNO", retorno);

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
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
					"[getRetornosAhSeremIgnorados] não foi possível verificar a quantidade de retornos a serem ignorados. id " + id + "produto "+produto
							+ e.getMessage() + "\n" + e.getCause());
		}
		
		if(qtd==null) {
			qtd = new BigDecimal(0);
		}
		
		return qtd;
	}
	
	private DynamicVO getContagem(BigDecimal numos, String tecla, BigDecimal produto) throws Exception {
		DynamicVO VS = null;
		
		try {
			JapeWrapper DAO = JapeFactory.dao("AD_APPCONTAGEM");
			DynamicVO VO = DAO.findOne("this.NUMOS=? AND this.CODPROD=? AND this.TECLA=?",new Object[] { numos, produto, tecla });
			if(VO!=null) {
				VS = VO;
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return VS;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_atualizaDadosVisita");
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
