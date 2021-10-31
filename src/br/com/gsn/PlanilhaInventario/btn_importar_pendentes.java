package br.com.gsn.PlanilhaInventario;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;
import com.sankhya.util.TimeUtils;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_importar_pendentes implements AcaoRotinaJava {
	
	/**
	 * 15/10/21 vs 1.0 Importar todos os registros pendentes da tela Inventário importado para a Contagem de estoque
	 * 30/10/21 vs 1.1 Inserido os métodos criarLinhaDeContagem e atualizaOuInsereContagem, refatoração do código.
	 */
	
	int importados = 0;
	int atualizados = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		verificaPendentes(arg0);
		arg0.setMensagemRetorno("<br/>Foram importados <b>"+importados+"</b> registrados.<br/>"
				+ "Foram atualizados <b>"+atualizados+"</b> registros.<br/>");
	}
	
	public void verificaPendentes(ContextoAcao arg0) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PLANINVENT","this.PENDENTE = ? ", new Object[] {"S"}));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);


			Timestamp data = (Timestamp) DynamicVO.getProperty("DTCONTAGEM");
			BigDecimal empresa = (BigDecimal) DynamicVO.getProperty("CODEMP");
			BigDecimal local = (BigDecimal) DynamicVO.getProperty("CODLOCAL");
			BigDecimal produto = (BigDecimal) DynamicVO.getProperty("CODPROD");
			String controle = (String) DynamicVO.getProperty("CONTROLE");
			BigDecimal quantidade = (BigDecimal) DynamicVO.getProperty("QUANTIDADE");
			String volume = (String) DynamicVO.getProperty("CODVOL");
			String tipo = (String) DynamicVO.getProperty("TIPO");
			BigDecimal parc = (BigDecimal) DynamicVO.getProperty("CODPARC");
			Timestamp dataValidade = (Timestamp) DynamicVO.getProperty("DTVAL");
			Timestamp dataFabricacao = (Timestamp) DynamicVO.getProperty("DTFABRICACAO");
			
			String observacao = (String) DynamicVO.getProperty("OBSERVACAO");
			observacao = "";
			
			boolean copia = validaSeExisteCopiaDeEstoque(data,empresa,local,produto,controle);
			
			if (copia) {

				observacao = atualizaOuInsereContagem(data,empresa,local,produto,controle,quantidade,volume,tipo,parc,dataValidade,dataFabricacao);	
				
			} else {
				boolean tgfest = validaSeExisteNaTGFEST(empresa, local, produto, controle);

				if (tgfest) { // existe na EST

					// copia
					observacao = criarLinhaDeContagem(data,empresa,local,produto,controle,quantidade,volume,tipo,parc,dataValidade,dataFabricacao);
					
					// contagem
					observacao = atualizaOuInsereContagem(data,empresa,local,produto,controle,quantidade,volume,tipo,parc,dataValidade,dataFabricacao);		

				} else { // Não existe
					inserirNaTGFEST(empresa, local, produto, controle, tipo, parc, dataFabricacao, dataValidade);
					observacao += "Produto inserido no estoque.\n";

					// copia
					observacao = criarLinhaDeContagem(data,empresa,local,produto,controle,quantidade,volume,tipo,parc,dataValidade,dataFabricacao);

					// contagem
					observacao = atualizaOuInsereContagem(data,empresa,local,produto,controle,quantidade,volume,tipo,parc,dataValidade,dataFabricacao);	
				}
			}
			
			salvaObservacao(data,empresa,local,produto,controle,observacao);
			
			}
			
		} catch (Exception e) {
			salvarException("[verificaPendentes] nao foi possível verificar os pendentes. data "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private String criarLinhaDeContagem(Timestamp data, BigDecimal empresa, BigDecimal local, BigDecimal produto, String controle, BigDecimal quantidade, String volume, String tipo,
			BigDecimal parc, Timestamp dataValidade, Timestamp dataFabricacao) {
		String observacao = "";
		
		try {
			
			criarLinhaDeContagem(data, empresa, local, produto, controle, new BigDecimal(0), volume, tipo, parc,
					dataValidade, dataFabricacao, new BigDecimal(1));
			observacao += "Inserido a cópia.\n";
			
		} catch (Exception e) {
			salvarException("[criarLinhaDeContagem] nao foi possível inserir a linha da contagem. data "+data+" produto "+produto+" empresa "+empresa+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		return observacao;
	}
	
	private String atualizaOuInsereContagem(Timestamp data, BigDecimal empresa, BigDecimal local, BigDecimal produto, String controle, BigDecimal quantidade, String volume, String tipo,
			BigDecimal parc, Timestamp dataValidade, Timestamp dataFabricacao) {
		
		String observacao = "";
		
		try {
			
			boolean contagem = validaSeExisteAhContagem(data, empresa, local, produto, controle);
			
			if(contagem) {
				atualizarContagem(data, empresa, local, produto, controle, quantidade);
				observacao += "Atualizado com Sucesso. \n";
				atualizados++;
			}else {
				criarLinhaDeContagem(data, empresa, local, produto, controle, quantidade, volume, tipo, parc,
						dataValidade, dataFabricacao, new BigDecimal(2));
				observacao += "Inserido a contagem.\n";

				importados++;
			}	
			
		} catch (Exception e) {
			salvarException("[atualizaOuInsereContagem] nao foi possível alterar ou inserir a contagem. data "+data+" produto "+produto+" empresa "+empresa+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		return observacao;
	}
	
	
	public void inserirNaTGFEST(BigDecimal empresa, BigDecimal local, BigDecimal produto, String controle, String tipo, BigDecimal parceiro, Timestamp dataFabricacao, Timestamp dataValidade) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("Estoque");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODEMP", empresa);
			VO.setProperty("CODLOCAL", local);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("CONTROLE", controle);
			VO.setProperty("RESERVADO", new BigDecimal(0));
			VO.setProperty("ESTMIN", new BigDecimal(0));
			VO.setProperty("ESTMAX", new BigDecimal(0));
			VO.setProperty("ATIVO", "S");
			
			if(dataValidade!=null) {
				VO.setProperty("DTVAL", dataValidade);
			}
			
			VO.setProperty("TIPO", tipo);
			
			if(parceiro!=null) {
				VO.setProperty("CODPARC", parceiro);
			}
			
			if(dataFabricacao!=null) {
				VO.setProperty("DTFABRICACAO", dataFabricacao);
			}
			
			VO.setProperty("STATUSLOTE", "N");
			VO.setProperty("ESTOQUE", new BigDecimal(0));
			VO.setProperty("AD_DTULTMOV", TimeUtils.getNow().toString());
			
			dwfFacade.createEntity("Estoque", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[inserirNaTGFEST] nao foi possível inserir na TGFEST! empresa "+empresa+" produto "+produto+" controle "+controle+" local "+local+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	public boolean validaSeExisteNaTGFEST(BigDecimal empresa,BigDecimal local,BigDecimal produto,String controle) {
		
		boolean valida=false;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) FROM TGFEST WHERE CODEMP="+empresa+" AND CODLOCAL="+local+" AND CONTROLE='"+controle+"' AND CODPROD="+produto);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}

			
		} catch (Exception e) {
			salvarException("[validaSeExisteCopiaDeEstoque] nao foi possível validar se a cópia de estoque existe! empresa "+empresa+" produto "+produto+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		return valida;
	}
	
	public void criarLinhaDeContagem(Timestamp data,BigDecimal empresa,BigDecimal local,BigDecimal produto,String controle, 
			BigDecimal quantidade, String volume, String tipo, BigDecimal parceiro, Timestamp dataValidade, Timestamp dataFabricacao, BigDecimal sequencia) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ContagemEstoque");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODPROD", produto);
			VO.setProperty("CODVOL", volume);
			VO.setProperty("QTDEST", quantidade);
			VO.setProperty("DTCONTAGEM", data);
			VO.setProperty("CONTROLE", controle);
			VO.setProperty("CODLOCAL", local);
			VO.setProperty("TIPO", tipo);
			
			if(parceiro==null) {
				VO.setProperty("CODPARC", new BigDecimal(0));
			}else {
				VO.setProperty("CODPARC", parceiro);
			}
			
			VO.setProperty("CODEMP", empresa);		
			
			if(dataValidade!=null) {
				VO.setProperty("DTVAL", dataValidade);
			}
			
			VO.setProperty("SEQUENCIA", sequencia);
			VO.setProperty("QTDESTUNCAD", quantidade);
			
			if(dataFabricacao!=null) {
				VO.setProperty("DTFABRICACAO", dataFabricacao);
			}
			
			dwfFacade.createEntity("ContagemEstoque", (EntityVO) VO);
			
		} catch (Exception e) {
			salvarException("[criarLinhaDeContagem] nao foi possível criar a linha na contagem! data "+data+" empresa "+empresa+" produto "+produto+" controle "+controle+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	public void atualizarContagem(Timestamp data,BigDecimal empresa,BigDecimal local,BigDecimal produto,String controle, BigDecimal quantidade) {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ContagemEstoque",
					"this.DTCONTAGEM=? AND this.CODEMP=? AND this.CODLOCAL=? AND this.CODPROD=? AND this.CONTROLE=? AND SEQUENCIA=?", new Object[] { 
							data, empresa, local, produto, controle, new BigDecimal(2) }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("QTDEST", quantidade);
				VO.setProperty("QTDESTUNCAD", quantidade);

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[atualizarContagem] nao foi possível atualizar a contagem! data "+data+" empresa "+empresa+" produto "+produto+" controle "+controle+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}

	public boolean validaSeExisteCopiaDeEstoque(Timestamp data,BigDecimal empresa,BigDecimal local,BigDecimal produto,String controle) {
		
		SimpleDateFormat formatador = new SimpleDateFormat("dd/MM/YYYY");
		Date dataX = new Date(data.getTime());
		String dataFormatada = formatador.format(dataX);
		
		boolean valida=false;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) FROM TGFCTE WHERE DTCONTAGEM='"+dataFormatada+"' AND CODEMP="+empresa+" AND CODLOCAL="+local+" AND CODPROD="+produto+" AND CONTROLE='"+controle+"' AND SEQUENCIA=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}

			
		} catch (Exception e) {
			salvarException("[validaSeExisteCopiaDeEstoque] nao foi possível validar se a cópia de estoque existe! data "+data+" empresa "+empresa+" produto "+produto+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		return valida;
	}
	
	public boolean validaSeExisteAhContagem(Timestamp data,BigDecimal empresa,BigDecimal local,BigDecimal produto,String controle) {
		
		SimpleDateFormat formatador = new SimpleDateFormat("dd/MM/YYYY");
		Date dataX = new Date(data.getTime());
		String dataFormatada = formatador.format(dataX);
		
		boolean valida=false;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) FROM TGFCTE WHERE DTCONTAGEM='"+dataFormatada+"' AND CODEMP="+empresa+" AND CODLOCAL="+local+" AND CODPROD="+produto+" AND CONTROLE='"+controle+"' AND SEQUENCIA=2");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}

			
		} catch (Exception e) {
			salvarException("[validaSeExisteAhContagem] nao foi possível validar se a contagem existe! data "+data+" empresa "+empresa+" produto "+produto+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		return valida;
	}
	
	public void salvaObservacao(Timestamp data, BigDecimal empresa, BigDecimal local, BigDecimal produto, String controle, String observacao) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PLANINVENT",
					"this.DTCONTAGEM=? AND this.CODEMP=? AND this.CODLOCAL=? AND this.CODPROD=? AND this.CONTROLE=?", new Object[] { data, empresa,local,produto,controle }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("OBSERVACAO", observacao);
				VO.setProperty("USUARIOATUALIZACAO", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
				VO.setProperty("DTATUALIZACAO", TimeUtils.getNow());
				VO.setProperty("PENDENTE", "N");

				itemEntity.setValueObject(NVO);
			}
	
		} catch (Exception e) {
			salvarException("[salvaObservacao] nao foi possível salvar a observação. data "+data+ " empresa "+empresa+" produto "+produto+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "btn_importar_pendentes");
			VO.setProperty("PACOTE", "br.com.gsn.PlanilhaInventario");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}

}
