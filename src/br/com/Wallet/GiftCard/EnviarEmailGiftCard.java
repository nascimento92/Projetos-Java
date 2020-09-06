package br.com.Wallet.GiftCard;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import com.sankhya.util.StringUtils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class EnviarEmailGiftCard implements ScheduledAction {

	public JapeSession.SessionHandle hnd; 
	
	public void onTime(ScheduledActionContext arg0) {
		
		hnd =null;
		try {
			
			hnd = JapeSession.open();
			hnd.execWithTX(new JapeSession.TXBlock() {

				public void doWithTx() throws Exception {
					
					start(); 
					
				}
				
			});
			
		} catch (Exception e) {
			System.out.println("Nao foi possivel enviar os Gift Card - Wallet! "+e.getMessage());
		}
	}
	
	private void start() throws Exception {
		verificaOsPendentesDeEnvioDeEmail();
	}
	
	private void verificaOsPendentesDeEnvioDeEmail() throws Exception {
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT CODIGO FROM AD_GIFTCARDGCW WHERE STATUSEMAIL IS NULL OR STATUSEMAIL='N'");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			BigDecimal codigo = contagem.getBigDecimal("CODIGO");
			getDados(codigo);
		}
	}
	
	private void getDados(BigDecimal codigo) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_GIFTCARDGCW","this.CODIGO = ? ", new Object[] { codigo }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
		
		String validadeFormatada = null;
		String cvv = DynamicVO.asString("CVV");
		String email = DynamicVO.asString("EMAIL");
		String nomeUsuario = DynamicVO.asString("HOLDER");
		String numeroCartao = DynamicVO.asString("CARDNUM");
		Timestamp validade = DynamicVO.asTimestamp("VALIDTHRU");
		
		if(validade!=null) {
			validadeFormatada = StringUtils.formatTimestamp(validade, "MM/YY");
		}

		String Mensagem = montaHtml(numeroCartao,nomeUsuario,validadeFormatada,cvv);
		
		if(numeroCartao!=null && validadeFormatada!=null && cvv!=null) {
			enviarEmail(Mensagem,email);
			
			DynamicVO.setProperty("STATUSEMAIL", "S");
			itemEntity.setValueObject((EntityVO) DynamicVO);
		}
		
		}

	}
	
	private void enviarEmail(String mensagem, String email) throws Exception {

		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
		DynamicVO VO = (DynamicVO) NPVO;
		
		VO.setProperty("CODFILA", getUltimoCodigoFila());
		VO.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
		VO.setProperty("MENSAGEM", mensagem.toCharArray());
		VO.setProperty("TIPOENVIO", "E");
		VO.setProperty("ASSUNTO", new String("Aproveite uma bebida quente com o seu amigo!"));
		VO.setProperty("EMAIL", email);
		VO.setProperty("CODUSU", new BigDecimal(0));
		VO.setProperty("STATUS", "Pendente");
		VO.setProperty("CODCON", new BigDecimal(0));		
		
		dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
	}
	
	private BigDecimal getUltimoCodigoFila() throws Exception {
		int count = 0;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(CODFILA)+1 AS CODFILA FROM TMDFMG");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("CODFILA");
		}
		
		BigDecimal ultimoCodigo = new BigDecimal(count);
		
		return ultimoCodigo;
	}
	
	private String montaHtml(String numeroCartao, String nome, String validade, String cvv) {
		String body="<p align=\"center\">" + 
				"	<! -- CABEÇALHO -->" + 
				"	<img src=\"https://i.ibb.co/KDZrpQf/cabe-alho.png\" width=\"640\"></img>" + 
				"	</p>" + 
				"" + 
				"	<p align=\"center\">" + 
				"	<! -- TÍTULO-->" + 
				"	<font color =\"#039963\" size=\"5\">" + 
				"	O CAFÉ FICA AINDA MELHOR SE DIVIDIRMOS COM UM <br>AMIGO, NÃO É MESMO?</p>" + 
				"	</font>" + 
				"</p>" + 
				"" + 
				"<table align=\"center\">" + 
				"	<tr>" + 
				"		<td bgcolor=\"#7ED258\">" + 
				"			<! -- SUBTÍTULO-->" + 
				"			<font color =\"white\" size=\"4,5\">" + 
				"			Por isso, estamos te enviando um gift card, para que você possa convidar<br>" + 
				"			um amigo ou alguém que queira conhecer melhor para bater um papo e tomar um café!" + 
				"			</font>" + 
				"		</td>" + 
				"	</tr>" + 
				"</table></p>"+
				""+
				"	<p align=\"center\">" + 
				"	<! -- TÍTULO-->" + 
				"	<font color =\"#039963\" size=\"5\">" + 
				"	É SIMPLES:" + 
				"	</font>" + 
				"</p>"+
				""+
				"	<p align=\"center\">" + 
				"	<! -- TÍTULO-->" + 
				"	<font color =\"black\" size=\"4\">" + 
				"	1. Baixe o nosso app, Gran Cofee Wallet" + 
				"	</font>" + 
				"</p>"+
				""+
				"	<p align=\"center\">" + 
				"	<a href=\"https://l.ead.me/gcappwallet\">"+ 
				"	<img src=\"https://i.ibb.co/CBb4z7w/wallet.png\" width =\"100\" height =\"100\"></a>"+
				"</p>"+
				""+
				"	<p align=\"center\">" + 
				"	<font color =\"black\" size=\"4\">" + 
				"	2. Convide um amigo" + 
				"	</font>" + 
				"</p>"+
				""+
				"	<p align=\"center\">" + 
				"	<a href='https://outlook.office365.com/mail/inbox'>"+ 
				"	<img src= \"https://i.ibb.co/cyjtrH8/mail.png\"width =\"100\" height =\"100\"></a>"+
				"</p>"+
				""+
				"	<p align=\"center\">" + 
				"	<font color =\"black\" size=\"4\">" + 
				"	3. Insira o código:" + 
				"	</font>" + 
				"</p>"+
				""+
				"<table align=\"center\">" + 
				"" + 
				"	<tr>" + 
				"		<td>" + 
				"			<font color =\"#039963\" size=\"3\">" + 
				"			NUMERO DO CARTÃO:" + 
				"			</font>" + 
				"		</td>" + 
				"	<td>" + 
				"		"+numeroCartao+"" + 
				"	</td>" + 
				"</tr>"+
				""+
				"	<tr>" + 
				"		<td>" + 
				"			<font color =\"#039963\" size=\"3\">" + 
				"			NOME:" + 
				"			</font>" + 
				"		</td>" + 
				"	<td>" + 
				"		"+nome+"" + 
				"	</td>" + 
				"</tr>"+
				""+
				"	<tr>" + 
				"		<td>" + 
				"			<font color =\"#039963\" size=\"3\">" + 
				"			EXPIRA EM:" + 
				"			</font>" + 
				"		</td>" + 
				"	<td>" + 
				"		"+validade+"" + 
				"	</td>" + 
				"</tr>"+
				""+
				"	<tr>" + 
				"		<td>" + 
				"			<font color =\"#039963\" size=\"3\">" + 
				"			CÓDIGO DE SEGURANÇA:" + 
				"			</font>" + 
				"		</td>" + 
				"	<td>" + 
				"		"+cvv+"" + 
				"	</td>" + 
				"</tr>"+
				"</table>"+
				""+
				"<p align=\"center\">" + 
				"	<font color =\"black\" size=\"4\">" + 
				"	4. Pague com o gift card as bebidas escolhidas:" + 
				"	</font>" + 
				"</p>"+
				""+
				"<p align=\"center\">" + 
				"	<br><img src=\"https://i.ibb.co/yg51YDB/gift.png\" alt=\"gift\" border=\"0\" width =\"153,31\" height =\"67,76\"></img>" + 
				"</p>"+
				""+
				"<p align=\"center\">" + 
				"	<font color =\"#039963\" size=\"5\">" + 
				"	 E AI, JÁ SABE QUEM VAI CONVIDAR?" + 
				" 	</font>" + 
				"</p>"+
				"<br>"+
				" <! --RODAPÉ-->"+
				"<p align=\"center\">"+
				"	<table style=\"background-color: #7ED258;\" name=\"blk_social_follow\" width=\"640\" class=\"blk\">"+
				"	<tbody>"+
				"		<tr>"+
				"			<td class=\"tblCellMain\" align=\"center\" style=\"padding-top:20; padding-bottom:20; padding-left:20; padding-right:20;\">"+
				"			<a href=\"https://www.instagram.com/grancoffeeoficial/\"><img src=\"https://i.ibb.co/2PnMY0x/instagram-2.png\"></a>"+
				"			<a href=\"https://pt-br.facebook.com/Grancoffeeoficial/\"><img src=\"https://i.ibb.co/bK10rW0/fb.png\"></a>"+
				"			<a href=\"https://www.linkedin.com/company/grancoffeeoficial/\"><img src=\"https://i.ibb.co/V2MQMjx/linkedin.png\"></a>"+
				"</td></tr></table>"+
				"</p>";
			
		return body;
	}

}
