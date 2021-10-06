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
import br.com.sankhya.extensions.actionbutton.Registro;
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

public class btn_importar_selecionadas implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		for(int i=0; i<linhas.length; i++) {
			start(linhas[i]);
		}
		
	}
	
	public void start(Registro linhas) throws Exception {
		Timestamp data = (Timestamp) linhas.getCampo("DTCONTAGEM");
		BigDecimal empresa = (BigDecimal) linhas.getCampo("CODEMP");
		BigDecimal local = (BigDecimal) linhas.getCampo("CODLOCAL");
		BigDecimal produto = (BigDecimal) linhas.getCampo("CODPROD");
		String controle = (String) linhas.getCampo("CONTROLE");
		BigDecimal quantidade = (BigDecimal) linhas.getCampo("QUANTIDADE");
		String volume = (String) linhas.getCampo("CODVOL");
		String tipo = (String) linhas.getCampo("TIPO");
		BigDecimal parceiro = (BigDecimal) linhas.getCampo("CODPARC");
		Timestamp dataValidade = (Timestamp) linhas.getCampo("DTVAL");
		Timestamp dataFabricacao = (Timestamp) linhas.getCampo("DTFABRICACAO");
		
		boolean copia = validaSeExisteCopiaDeEstoque(data,empresa,local,produto,controle);
		
		if(copia) {
			
			boolean contagem = validaSeExisteAhContagem(data,empresa,local,produto,controle);
			
			if(contagem) { //Atualizar
				atualizarContagem(data,empresa,local,produto,controle,quantidade, linhas);
			}else { //Inserir
				criarLinhaDeContagem(data,empresa,local,produto,controle,quantidade,linhas,volume,tipo,parceiro,dataValidade,dataFabricacao);	
			}	
		}else {
			
			//1° validar se existe na tgfest
			//2° inserir na Est se não tiver, inserir a copia.
			// boolean tgfest = validaSeExisteNaTGFEST(empresa,local,produto,controle);
			
			linhas.setCampo("OBSERVACAO", "Não existe uma cópia de estoque para este item");
		}
	}
	
	public void criarLinhaDeContagem(Timestamp data,BigDecimal empresa,BigDecimal local,BigDecimal produto,String controle, 
			BigDecimal quantidade, Registro linhas, String volume, String tipo, BigDecimal parceiro, Timestamp dataValidade, Timestamp dataFabricacao) {
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
			
			VO.setProperty("SEQUENCIA", new BigDecimal(2));
			VO.setProperty("QTDESTUNCAD", quantidade);
			
			if(dataFabricacao!=null) {
				VO.setProperty("DTFABRICACAO", dataFabricacao);
			}
			
			dwfFacade.createEntity("ContagemEstoque", (EntityVO) VO);
			
			linhas.setCampo("OBSERVACAO", "Inserido com Sucesso");
		} catch (Exception e) {
			salvarException("[criarLinhaDeContagem] nao foi possível criar a linha na contagem! data "+data+" empresa "+empresa+" produto "+produto+" controle "+controle+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	public void atualizarContagem(Timestamp data,BigDecimal empresa,BigDecimal local,BigDecimal produto,String controle, BigDecimal quantidade, Registro linhas) {
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
				linhas.setCampo("OBSERVACAO", "Atualizado com Sucesso");

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			salvarException("[atualizarContagem] nao foi possível atualizar a contagem! data "+data+" empresa "+empresa+" produto "+produto+" controle "+controle+"\n"+e.getMessage()+"\n"+e.getCause());
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
			nativeSql.appendSql("SELECT COUNT(*) FROM TGFCTE WHERE DTCONTAGEM='"+dataFormatada+"' AND CODEMP="+empresa+" AND CODLOCAL="+local+" AND CODPROD="+produto+" AND CONTROLE='"+controle+"' AND SEQUENCIA=1");
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
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_importar_selecionadas");
			VO.setProperty("PACOTE", "br.com.gsn.PlanilhaInventario");
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
